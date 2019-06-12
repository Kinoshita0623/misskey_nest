package org.panta.misskeynest.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.panta.misskeynest.entity.EmojiProperty
import org.panta.misskeynest.util.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL

class CustomEmoji(private val context: Context){

    companion object{

        private val bitmapCache = BitmapCache(10)


    }

    private val svgParser = SVGParser(bitmapCache)
    private val emojiFileList = context.fileList().map{
        File(context.filesDir, it)
    }
    private var emojiMap = emojiFileList.map{
        it.name.replace(":", "").split(".")[0] to it
    }.toMap()

    private val spannableCache = SpannableStringBuilderCache()

    fun updateEmojiMap(){
        val emojiFileList = context.fileList().map{
            File(context.filesDir, it)
        }
        emojiMap = emojiFileList.map{
            it.name.replace(":", "").split(".")[0] to it
        }.toMap()
    }

    fun setTextView(textView: TextView, text: String, notesEmojiList: List<EmojiProperty>? = null){
        val cache = spannableCache.get(text)
        if(cache != null){
            textView.text = cache
            textView.visibility = View.VISIBLE
            return
        }
        Handler(Looper.getMainLooper()).post{
            textView.visibility = View.INVISIBLE
        }
        GlobalScope.launch {
            try{
                val spannable = SpannableStringBuilder()

                val charArray = text.toCharArray()
                val iterator = charArray.iterator()

                var charTmp = StringBuilder()
                while(iterator.hasNext()){
                    val c = iterator.next()

                    if(c == ':'){

                        val midwayText = charTmp.toString()
                        val emojiFile = getEmojisFile(midwayText)
                        val notesEmoji = notesEmojiList?.firstOrNull{ it.name == midwayText }
                        if(emojiFile != null){
                            //ローカルストレージに存在する場合
                            appendImageSpanFromFile(spannable, midwayText, emojiFile, textView.textSize.toInt())
                        }else if(notesEmoji != null){
                            //ローカルストレージに存在しない場合
                            appendImageSpanFromEmojiProperty(spannable, midwayText, notesEmoji, textView.textSize.toInt())
                        }else{
                            //通常の文字列だった場合
                            spannable.append(midwayText)
                            spannable.append(c)
                        }

                        charTmp = StringBuilder()


                    }else{
                        charTmp.append(c)
                    }
                }

                //最後に残ったテキストを解析
                val last = charTmp.toString()
                val emojiFile = emojiMap[last.replace(":", "")]
                if(emojiFile != null){
                    appendImageSpanFromFile(spannable, last, emojiFile, textView.textSize.toInt())
                }else{
                    spannable.append(last)
                }

                spannableCache.put(text, spannable)
                Handler(Looper.getMainLooper()).post{
                    textView.text = spannable
                    textView.visibility = View.VISIBLE
                }

            }catch(e: Exception){
                Log.d("CustomEmoji", "error", e)
            }

        }

    }

    fun getEmojisFile(emoji: String): File?{
        return emojiMap[emoji.replace(":", "")]
    }

    private suspend fun getEmojisBitmap(emojiProperty: EmojiProperty, size: Int): Bitmap{

        val meta = emojiProperty.getExtension()
        val bitmap = if(meta == "svg"){
            val textSvg = emojiProperty.saveSVG(context.openFileOutput(emojiProperty.createFileName(), Context.MODE_PRIVATE))
            svgParser.getBitmapFromString(textSvg, size, size)
        }else{
            resizeBitmap(emojiProperty.saveImage(context.openFileOutput(emojiProperty.createFileName(), Context.MODE_PRIVATE)), size)
        }
        updateEmojiMap()
        return bitmap
    }

    private fun getEmojisBitmap(emojiFile: File, size: Int): Bitmap{

        return if(emojiFile.path.endsWith(".svg")){
            svgParser.getBitmapFromFile(emojiFile, size, size)
        }else{
            resizeBitmap(BitmapFactory.decodeFile(emojiFile.path), size)
        }
    }



    private fun appendImageSpanFromFile(spannable: SpannableStringBuilder, text: String, emojiFile: File, size: Int){
        try{
            val finalSize = (size.toDouble() * 1.2).toInt()
            val bitmap = getEmojisBitmap(emojiFile, finalSize)

            appendImageSpanFromBitmap(spannable, text, bitmap)
        }catch(e: Exception){
            Log.d("CustomEmoji", "appendImageSpan method. エラー発生 text:$text, emojiFile: ${emojiFile.path}")
        }
    }


    private suspend fun appendImageSpanFromEmojiProperty(spannable: SpannableStringBuilder, text: String, emoji: EmojiProperty, size: Int){
        try{
            val finalSize = (size.toDouble() * 1.2).toInt()
            val bitmap = getEmojisBitmap(emoji, finalSize)

            appendImageSpanFromBitmap(spannable, text, bitmap)
        }catch(e: Exception){
            Log.d("CustomEmoji", "appendImageSpan method. エラー発生 text:$text, emoji$emoji}")
        }
    }

    private fun appendImageSpanFromBitmap(spannable: SpannableStringBuilder, text: String, bitmap: Bitmap){
        try{
            val imageSpan = ImageSpan(context, bitmap)
            val start = spannable.length
            spannable.append(text)
            spannable.setSpan(imageSpan, start - 1, start + text.length , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }catch(e: Exception){
            Log.d("CustomEmoji", "appendImageSpan method. エラー発生")

        }
    }


    private fun resizeBitmap(bitmap: Bitmap, size: Int?): Bitmap{
        val tmpSize = if(bitmap.width < 50){
            size ?: 80
        }else{
            size
        }

        return if(tmpSize == null){
            bitmap
        }else{
            val scale = tmpSize / bitmap.width.toDouble()
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        }
    }
}