package com.example.testprinter

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import android.widget.Button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileOutputStream

fun log(msg: String){
    Log.i("Printer", msg)
}

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener{ Printer().print(this) }

        runMemoryLoad()
    }


    //This is to simulate memory consumption and motivate OS to kill our app faster
    //Adjust based on your device performance
    private fun runMemoryLoad() {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                val array = IntArray(1024*1024*5) { _ -> 0}
                while (!isFinishing){
                    for (i in array.indices) {
                        array[i] = array[i]+1
                    }
                    log("Done a piece of useless work ${array[0]}")
                    delay(200)
                }
                log("Exiting useless work")
            }
        }
    }
}



class Printer {

    fun print(context : Context) {
        log("Printer.print")
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val attrib = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()

        printManager.print("Test print job name", SampleDocumentAdapter(context, "sample.pdf"), attrib)
        log("Printer.print done")
    }

    private inner class SampleDocumentAdapter(
        private val context : Context,
        private val assetFileName: String
    ) : PrintDocumentAdapter(){

        override fun onStart() {
            log("SampleDocumentAdapter.onStart")
            super.onStart()
        }

        override fun onFinish() {
            log("SampleDocumentAdapter.onFinish")
            super.onFinish()
        }

        override fun onLayout(oldAttrs: PrintAttributes?, newAttrs: PrintAttributes,cancelSignal: CancellationSignal,
            callback: LayoutResultCallback, extra: Bundle?)
        {
            log("SampleDocumentAdapter.onLayout")
            if (cancelSignal.isCanceled) {
                callback.onLayoutCancelled()
            } else {
                PrintDocumentInfo.Builder("Test printer output")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                    .also {
                        callback.onLayoutFinished(it, true)
                    }
            }
            log("SampleDocumentAdapter.onLayout done")
        }

        override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor,
                             cancelSignal: CancellationSignal, callback: WriteResultCallback)
        {

            log("SampleDocumentAdapter.onWrite")
            CoroutineScope(Dispatchers.IO).launch {
                launch {
                    log("SampleDocumentAdapter.onWrite async")

                    try {
                        context.assets.open(assetFileName).use { inStream ->
                            val buffer = ByteArray(16384)

                            FileOutputStream(destination.fileDescriptor).use { outStream ->
                                var totalBytes = 0
                                while (true) {
                                    val length = inStream.read(buffer)
                                    if (length < 0) break
                                    outStream.write(buffer, 0, length)
                                    totalBytes += length
                                }
                                log("SampleDocumentAdapter.onWrite - total of $totalBytes bytes written")
                            }
                        }

                        destination.close()
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

                        log("SampleDocumentAdapter.onWrite done")
                    } catch (e: Exception){
                        log("SampleDocumentAdapter.onWrite error $e")
                        callback.onWriteFailed("SampleDocumentAdapter.onWrite error $e")
                    }
                }
            }

            log("SampleDocumentAdapter.onWrite done")
        }
    }

}

