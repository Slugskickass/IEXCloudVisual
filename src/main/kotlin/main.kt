import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.apache.commons.csv.CSVFormat

import org.apache.commons.csv.CSVParser
import org.jetbrains.skija.Image
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder
import pl.zankowski.iextrading4j.client.IEXTradingApiVersion
import pl.zankowski.iextrading4j.client.IEXTradingClient
import pl.zankowski.iextrading4j.client.rest.request.stocks.QuoteRequestBuilder
import java.nio.file.Files
import java.nio.file.Paths
import pl.zankowski.iextrading4j.client.rest.request.stocks.LogoRequestBuilder

import pl.zankowski.iextrading4j.api.stocks.Logo
import pl.zankowski.iextrading4j.client.IEXCloudClient
import java.io.File
import java.nio.file.StandardCopyOption

import java.io.InputStream
import java.net.URL

import androidx.compose.desktop.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap





val basefilename = "/Users/Ashley/IdeaProjects/IEXCloudVisual/src/resources/\n"


fun main() = Window {

    val cloudClient = IEXTradingClient.create(
        IEXTradingApiVersion.IEX_CLOUD_STABLE,
        IEXCloudTokenBuilder().withPublishableToken(token).build()
    )
    val csvData = "src/resources/stocks.csv"
    val out = loadcsv(csvData)
    Scaffold(
        topBar = { TopAppBar(title = {Text("TopAppBar")},backgroundColor = Color.LightGray)  },
        drawerContent = { Text(text = "drawerContent") },
        bodyContent = { buildstocks(out, cloudClient) },
        bottomBar = { BottomAppBar(backgroundColor = Color.DarkGray) { Text("BottomAppBar") } }
    )

    }


public class Stock(val id: Int, var name: String, val number_held: Int, val stock_currency: String,
                   val start_val: Double, val current_val: Double, val current_return: Double, val current_holding: Double)

fun loadcsv(filename: String): MutableList<Stock> {
    val reader = Files.newBufferedReader(Paths.get(filename))
    val csvParser = CSVParser(reader, CSVFormat.DEFAULT)
    var stocks = mutableListOf<Stock>()

    var counter = 1
    for ( item in csvParser) {
        stocks.add(Stock(counter,item.get(0),item.get(2).toInt(),"GBP",item.get(1).substring(1).toDouble(),1.0,1.0,1.0))
        counter = counter + 1
    }

    return stocks
}

fun fixnames(name: String): String {
    var newname = ""
    var listname = name.toString().split(':')
    if (listname.component1().toString().contains("LSE")) {newname = listname.component2() + "-LN"}
    if (listname.component1().toString().contains("NYSE")) {newname = listname.component2()}
    return newname
}

fun getimagefile(name: String, tradingclient: IEXCloudClient): String {
    val FILE_NAME = basefilename + name + ".png"
    val file = File(FILE_NAME)
    if (file.exists()){
        println("File exists")}
    else{
    val logo: Logo = tradingclient.executeRequest(LogoRequestBuilder().withSymbol(name).build())
    val `in`: InputStream = URL(logo.url).openStream()
    Files.copy(`in`, Paths.get(FILE_NAME), StandardCopyOption.REPLACE_EXISTING)
    }
    return FILE_NAME
}

fun imageFromFile(file: File): ImageBitmap {
    return Image.makeFromEncoded(file.readBytes()).asImageBitmap() }

@Composable fun buildstocks(out: MutableList<Stock>, cloudClient: IEXCloudClient){
    ScrollableColumn  {
    for (item in out) {
        getimagefile(fixnames(item.name), cloudClient)
        showstock(item)
    }
}
}

@Composable fun showstock(currentstock: Stock){
    val tempfilename = basefilename + fixnames(currentstock.name) + ".png"
    val image =  imageFromFile(File(tempfilename))
    Row {
        Image(
        bitmap = image
        )
        Column {
            Text(fixnames(currentstock.name))
            Text(currentstock.current_val.toString())
        }
    }
}
