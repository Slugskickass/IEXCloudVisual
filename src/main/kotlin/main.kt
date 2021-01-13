import androidx.compose.desktop.Window
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.zankowski.iextrading4j.api.refdata.v1.Currency
import pl.zankowski.iextrading4j.api.stocks.v1.PriceTarget
import pl.zankowski.iextrading4j.client.rest.request.stocks.PriceRequestBuilder
import java.math.BigDecimal
import java.math.RoundingMode


val basefilename = "/Users/Ashley/IdeaProjects/IEXCloudVisual/src/resources/\n"


fun main() = Window() {
    val cloudClient = IEXTradingClient.create(
        IEXTradingApiVersion.IEX_CLOUD_STABLE,
        IEXCloudTokenBuilder().withPublishableToken(token).build()
    )
    val csvData = "src/resources/stocks.csv"
    var out = initialstocks(loadcsv(csvData), cloudClient)




    Scaffold(
        topBar = { TopAppBar(title = {Text("The total value of the Portfolio is ${gettotal(out).setScale(2, RoundingMode.HALF_DOWN).toString()}")},backgroundColor = Color.LightGray)  },
        drawerContent = { Text(text = "drawerContent") },
        bodyContent = { buildstocks(out, cloudClient) },
        bottomBar = { BottomAppBar(backgroundColor = Color.DarkGray) { Text("BottomAppBar") } }
    )

    }

public class Stock(
    val id: Int, var name: String, val number_held: Int, val stock_currency: String,
    var start_val: Double, var current_val: BigDecimal, val current_return: Double, var current_holding: BigDecimal)

fun loadcsv(filename: String): MutableList<Stock> {
    val reader = Files.newBufferedReader(Paths.get(filename))
    val csvParser = CSVParser(reader, CSVFormat.DEFAULT)
    var stocks = mutableListOf<Stock>()

    var counter = 1
    for ( item in csvParser) {
        stocks.add(Stock(counter,item.get(0),item.get(2).toInt(),"GBP",item.get(1).substring(1).toDouble(),1.0.toBigDecimal(),1.0,1.00.toBigDecimal()))
        counter = counter + 1
    }

    return stocks
}

fun fixnames(name: String): String {
    var newname = name
    var listname = name.toString().split(':')
    if (name.contains(':')){
        if (listname.component1().toString().contains("LSE")) {newname = listname.component2() + "-LN"}
        if (listname.component1().toString().contains("NYSE")) {newname = listname.component2()}
    }
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

fun initialstocks(stocks: MutableList<Stock>, cloudClient: IEXCloudClient): MutableList<Stock> {
    stocks.map { it.name =  fixnames(it.name) }
    stocks.map { it.start_val = it.start_val / it.number_held }
    stocks.map {it.current_val = getcurrentvalue(it.name, cloudClient) }
    stocks.map {it.current_holding = it.current_val * it.number_held.toBigDecimal()}
    return stocks
}

fun getcurrentvalue(name: String, cloudClient: IEXCloudClient): BigDecimal {
    val quote = cloudClient.executeRequest(QuoteRequestBuilder().withSymbol(name).build())
    //val quote = cloudClient.executeRequest(PriceRequestBuilder().withSymbol(name).build())
    var scalefactor = 1.0
    if(quote.primaryExchange.contains("LONDON")){scalefactor = 1.0}
    if(quote.primaryExchange.contains("London")){scalefactor = 1.0}
    if(quote.primaryExchange.contains("NEW YORK")){scalefactor = 0.73}
    return quote.latestPrice.toDouble().toBigDecimal() * scalefactor.toBigDecimal()
}

fun gettotal(stocks: MutableList<Stock>): BigDecimal {
    val total_value = stocks.sumOf { it.current_holding }
return total_value
}

@Composable fun buildstocks(out: MutableList<Stock>, cloudClient: IEXCloudClient){
    ScrollableColumn  {
    for (item in out) {
        getimagefile((item.name), cloudClient)
        Divider()
        showstock(item)
    }
}
}

@Composable fun showstock(currentstock: Stock) {
    val tempfilename = basefilename + (currentstock.name) + ".png"
    val image = imageFromFile(File(tempfilename))

    val mycolour = if (currentstock.current_val - currentstock.start_val.toBigDecimal() > 0.0.toBigDecimal())
    {
        //Color.Green
        Color(0xFFA5D6A7)
    }
    else{
        //Color.Red
        Color(0xFFEF9A9A)
    }

    Card(Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(8.dp), backgroundColor = mycolour) {

        Row {
            Spacer(modifier = Modifier.width(16.dp))
            Image(
                bitmap = image, modifier = Modifier.size(48.dp, 48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(modifier = Modifier.width(160.dp)) {
                    Text(fixnames(currentstock.name), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentstock.stock_currency, textAlign = TextAlign.Center)
                }
                Row(modifier = Modifier.width(320.dp)) {
                    Text(currentstock.start_val.toBigDecimal().setScale(2, RoundingMode.HALF_DOWN).toString(), textAlign = TextAlign.Center, style = TextStyle(fontSize = 16.sp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentstock.current_val.setScale(2, RoundingMode.HALF_DOWN).toString(), textAlign = TextAlign.Center, style = TextStyle(fontSize = 16.sp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentstock.number_held.toString(), textAlign = TextAlign.Center, style = TextStyle(fontSize = 16.sp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currentstock.current_holding.setScale(2, RoundingMode.HALF_DOWN).toString(), textAlign = TextAlign.Center, style = TextStyle(fontSize = 16.sp))

                }
            }
        }
    }
}

