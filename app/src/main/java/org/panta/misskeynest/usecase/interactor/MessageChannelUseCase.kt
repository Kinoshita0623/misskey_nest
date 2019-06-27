package org.panta.misskeynest.usecase.interactor

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.*
import org.panta.misskeynest.entity.ConnectionProperty
import org.panta.misskeynest.entity.MessageProperty
import org.panta.misskeynest.entity.StreamingProperty
import org.panta.misskeynest.interfaces.CallBackListener
import org.panta.misskeynest.interfaces.IItemFilter
import org.panta.misskeynest.usecase.IMessageChannelUseCase
import org.panta.misskeynest.viewdata.MessageViewData

private const val TAG = "MessageChannelUseCase"
class MessageChannelUseCase(private val mConnectionProperty: ConnectionProperty, private val mFilter: IItemFilter<MessageProperty, MessageViewData>) : IMessageChannelUseCase{

    override var messageReceivedListener: CallBackListener<List<MessageViewData>>? = null

    private val mClient = OkHttpClient()
    private var mWebSocket: WebSocket? = null

    override fun start() {
        val wssDomain = mConnectionProperty.domain.replace("https://", "wss://")

        val request = Request.Builder()
            .url(wssDomain)
            .build()
        mWebSocket = mClient.newWebSocket(request, webSocketListener)

    }


    private val webSocketListener = object : WebSocketListener(){
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "onOpen")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d(TAG, "onFailure")
            Thread.sleep(1000)
            start()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            if(text.isBlank()) return

            try{
                val obj: StreamingProperty<Any> = jacksonObjectMapper().readValue(text)
                val body = obj.body.body
                Log.d(TAG, "受信した内容 :$body")
                if( obj.type != "channel") return
                if( obj.body.type == "messagingMessage"){
                    val messageProperty: MessageProperty = jacksonObjectMapper().readValue(obj.body.body.toString())

                    val viewData= mFilter.filter(messageProperty)
                    messageReceivedListener?.callBack(listOf(viewData))
                }
            }catch(e: Exception){
                Log.d(TAG, "onMessageでエラー発生", e)
            }


           // StreamingProperty<MessageProperty>()
        }
    }


}