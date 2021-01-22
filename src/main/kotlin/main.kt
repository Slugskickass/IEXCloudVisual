import androidx.compose.desktop.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.skija.Image
import pl.zankowski.iextrading4j.api.stocks.Logo
import pl.zankowski.iextrading4j.client.IEXCloudClient
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder
import pl.zankowski.iextrading4j.client.IEXTradingApiVersion
import pl.zankowski.iextrading4j.client.IEXTradingClient
import pl.zankowski.iextrading4j.client.rest.request.stocks.LogoRequestBuilder
import pl.zankowski.iextrading4j.client.rest.request.stocks.QuoteRequestBuilder
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.concurrent.fixedRateTimer


val basefilename = "/Users/Ashley/IdeaProjects/IEXCloudVisual/src/resources/\n"


fun main() = Window() {
    val cloudClient = IEXTradingClient.create(
        IEXTradingApiVersion.IEX_CLOUD_STABLE_SANDBOX,
        IEXCloudTokenBuilder().withPublishableToken(tokens).build()
    )
    val csvData = "src/resources/stocks.csv"

    var out = initialstocks(loadcsv(csvData), cloudClient)
    playtimer(out, cloudClient)


    Scaffold(
        topBar = { TopAppBar(title = {Text("The total value of the Portfolio is ${gettotal(out).setScale(2, RoundingMode.HALF_DOWN).toString()}")},backgroundColor = Color.LightGray)  },
        drawerContent = { Text(text = "drawerContent") },
        bodyContent = { buildstocks(out, cloudClient) },
        bottomBar = { BottomAppBar(backgroundColor = Color.DarkGray) { displayButtons(out, cloudClient) }} //Button(modifier = Modifier.align(Alignment.CenterVertically), onClick = {println("test")}){Text("Hello")}
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

fun playtimer(stocks: MutableList<Stock>, cloudClient: IEXCloudClient) {
    val fixedRateTimer = fixedRateTimer(name = "my timer", initialDelay = 10000, period = 30000){
        updateprice(stocks, cloudClient)
    }
}

fun updateprice(stocks: MutableList<Stock>, cloudClient: IEXCloudClient) {

    for (item in stocks){
        val holddata = item
        val pos = stocks.indexOfFirst { it.name == holddata.name}
        holddata.current_val = 0.0.toBigDecimal()//getcurrentvalue(holddata.name, cloudClient)
        holddata.current_holding = holddata.current_val * holddata.number_held.toBigDecimal()
        stocks[pos] = holddata
        println(item.name)
        println(holddata.name)
    }

//    stocks.map {it.current_val = getcurrentvalue(it.name, cloudClient) }
//    stocks.map {it.current_holding = it.current_val * it.number_held.toBigDecimal()}
//    print(stocks.component1().name)
//    print(" ")
//    println(stocks.component1().current_val)
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

fun printoutstocks(stocks: MutableList<Stock>){
    stocks.forEach { println("Name: ${it.name} current value is ${it.current_val}") }
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
            Button(onClick = {println("TEST")}) {Text("Button")}
        }
    }
}

@Composable fun displayButtons(stocks: MutableList<Stock>, cloudClient: IEXCloudClient){
    Row(){
        updatebutton(stocks, cloudClient)
        loadbutton()
        savebutton()
        testbutton(stocks)
    }
}

@Composable fun updatebutton(stocks: MutableList<Stock>, cloudClient: IEXCloudClient){
    Button( onClick = {updateprice(stocks, cloudClient)
        println("test")     }){Text("Update")}
}

@Composable fun loadbutton(){
    Button( onClick = {println("test")}){Text("Load")}
}

@Composable fun savebutton(){
    Button( onClick = {println("test")}){Text("Save")}
}

@Composable fun testbutton(stocks: MutableList<Stock>){
    Button( onClick = {printoutstocks(stocks)}){Text("Test")}
}



