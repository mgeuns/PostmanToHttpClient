import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Serializable
data class Url(val raw: String)

@Serializable
data class Raw(val language: String? = null)

@Serializable
data class Options(val raw: Raw? = null)

@Serializable
data class Formdata(val key: String, val value: String? = null, val contentType: String? = null, val type: String, val src: String? = null)

@Serializable
data class Body(val mode: String, val formdata: List<Formdata>? = null, val raw: String? = null, val options: Options? = null)

@Serializable
data class Header(val key: String, val value: String, val type: String, val disabled: Boolean)

@Serializable
data class Request(val method: String, val header: List<Header>, val body: Body? = null, val url: Url)

@Serializable
data class DataItem(val name: String, val request: Request? = null)

@Serializable
data class ControllerItem(val name: String, val item: List<DataItem>? = null, val request: Request? = null)

@Serializable
data class HeadItem(val name: String? = null, val item: List<ControllerItem>)

@Serializable
data class JsonObject(val item: List<HeadItem>)


fun main(args: Array<String>) {
    val jsonContent = File("/Users/maikelgeuns/ivho/berichten-service/src/test/postman_files/Berichten service.postman_collection.json").readText()
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true

    }

    val jsonObject = json
        .decodeFromString<JsonObject>(jsonContent)

    create_folders(jsonObject)
}


fun create_folders(jsonObject: JsonObject) {
    for (item in jsonObject.item) {
        var path = "src/main/resources"
        path = path + "/" + item.name
        Files.createDirectories(Paths.get(path))

        item.item.forEach { controllerItem ->
            if (controllerItem.item != null) {
                val subPath = "$path/${toCamelCase(controllerItem.name)}.http"
                Files.createFile(Paths.get(subPath))

                controllerItem.item.forEach { dataItem ->
                    if(dataItem.request != null) {
                        createHttpRequest(dataItem.request, subPath, dataItem.name)
                    }
                }

            } else if (controllerItem.request != null) {
                val subPath = path + "/" + toCamelCase(controllerItem.name) + ".http"
                Files.createFile(Paths.get(subPath))

                createHttpRequest(controllerItem.request, subPath, controllerItem.name)
            }
        }
    }
}

fun createHttpRequest(request: Request, file: String, name: String) {
    val fileToEdit = File(file)
    fileToEdit.appendText("###$name\n")
    fileToEdit.appendText(request.method + " " + request.url.raw + "\n")

    if (request.body != null) {
        if (request.body.mode == "formdata") {

            fileToEdit.appendText("Content-Type: multipart/form-data; boundary=boundary\n\n")

            request.body.formdata!!.forEach { formdata ->
                fileToEdit.appendText("\n--boundary\n")

                if (formdata.type == "text" || formdata.type == "default" || formdata.contentType == "application/json") {
                    fileToEdit.appendText( "Content-Disposition: form-data; name=\"${formdata.key}\"\n")
                    fileToEdit.appendText("Content-Type: application/json\n")
                    fileToEdit.appendText("\n" + formdata.value)
                } else if (formdata.type == "file") {
                    fileToEdit.appendText("Content-Disposition: form-data; name=\"${formdata.key}\"; filename=\"${formdata.src!!.substringAfterLast("/")}\"\n")
                    fileToEdit.appendText("\n<" + " ${formdata.src}")
                } else {
                    println("NOT IMPLEMENTED")
                }
                fileToEdit.appendText("\n")
                fileToEdit.appendText("--boundary")
                fileToEdit.appendText("\n")
            }


        } else if (request.body.mode == "raw") {
            fileToEdit.appendText("Content-Type: application/json\n")
            fileToEdit.appendText("\n")
            fileToEdit.appendText(request.body.raw!! + "\n")
        }
    }

    fileToEdit.appendText("\n")



}

fun toCamelCase(input: String): String {
    return input.split(" ", "-")
        .joinToString(separator = "") { it.capitalize() }
}