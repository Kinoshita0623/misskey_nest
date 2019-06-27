package org.panta.misskeynest.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_message.*
import org.panta.misskeynest.R
import org.panta.misskeynest.adapter.MessageAdapter
import org.panta.misskeynest.contract.MessageContract
import org.panta.misskeynest.entity.ConnectionProperty
import org.panta.misskeynest.filter.MessageFilter
import org.panta.misskeynest.interfaces.ErrorCallBackListener
import org.panta.misskeynest.interfaces.IOperationAdapter
import org.panta.misskeynest.presenter.MessagePresenter
import org.panta.misskeynest.repository.remote.MessagePagingRepository
import org.panta.misskeynest.repository.remote.MessageRepository
import org.panta.misskeynest.viewdata.MessageViewData

class MessageFragment : Fragment(), MessageContract.View {

    companion object{
        private const val MESSAGE_DATA_KEY = "MessageFragmentMessageViewDataKey"
        private const val CONNECTION_PROPERTY_KEY = "MessageFragmentConnectionProperty"

        fun getInstance(connectionProperty: ConnectionProperty, messageViewData: MessageViewData): MessageFragment{
            return MessageFragment().apply{
                val bundle = Bundle()
                bundle.putSerializable(MESSAGE_DATA_KEY, messageViewData)
                bundle.putSerializable(CONNECTION_PROPERTY_KEY, connectionProperty)
                arguments = bundle
            }
        }

    }

    override var mPresenter: MessageContract.Presenter? = null

    private var mAdapter: IOperationAdapter<MessageViewData>? = null

    private lateinit var mLayoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_message, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getSerializable( MESSAGE_DATA_KEY ) as MessageViewData
        val group = message.message.group
        val recipient = message.message.recipient

        val connectionProperty = arguments?.getSerializable( CONNECTION_PROPERTY_KEY ) as ConnectionProperty

        val pagingRepository = MessagePagingRepository(groupId = group?.id, userId = recipient?.id, connectionProperty = connectionProperty)
        val messageRepository = MessageRepository(connectionProperty)

        val filter = MessageFilter(connectionProperty)

        mPresenter = MessagePresenter(this,  messageRepository, pagingRepository, filter)

        mPresenter?.start()

        mLayoutManager = LinearLayoutManager(context)

    }

    override fun showMessage(list: List<MessageViewData>) {
        Handler(Looper.getMainLooper()).post{
            val adapter = MessageAdapter(list)
            messages_view.layoutManager = mLayoutManager
            messages_view.adapter = adapter

            mAdapter = adapter

        }
    }

    override fun showNewMessage(list: List<MessageViewData>) {
        Handler(Looper.getMainLooper()).post{
            mAdapter?.addAllLast(list)
        }
    }

    override fun showOldMessage(list: List<MessageViewData>) {
        Handler(Looper.getMainLooper()).post{
            mAdapter?.addAllFirst(list)
        }
    }



    private val errorListener = object : ErrorCallBackListener{
        override fun callBack(e: Exception) {
            Log.w("", "error", e)
        }
    }

}