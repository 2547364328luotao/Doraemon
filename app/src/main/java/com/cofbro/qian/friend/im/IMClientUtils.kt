package com.cofbro.qian.friend.im

import cn.leancloud.LCObject
import cn.leancloud.LCQuery
import cn.leancloud.LCUser
import cn.leancloud.im.v2.LCIMClient
import cn.leancloud.im.v2.LCIMConversation
import cn.leancloud.im.v2.LCIMException
import cn.leancloud.im.v2.LCIMMessage
import cn.leancloud.im.v2.callback.LCIMClientCallback
import cn.leancloud.im.v2.callback.LCIMConversationCallback
import cn.leancloud.im.v2.callback.LCIMConversationCreatedCallback
import cn.leancloud.im.v2.callback.LCIMConversationQueryCallback
import cn.leancloud.im.v2.callback.LCIMMessagesQueryCallback
import cn.leancloud.im.v2.messages.LCIMTextMessage
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.Arrays


object IMClientUtils {
    private var client: LCIMClient? = null
    private var user: LCUser? = null

    fun getIMClient(): LCIMClient? {
        return client
    }

    fun getCntUser(): LCUser? {
        return user
    }

    /**
     * 登录即时通信
     * @param lUsername lcUsername
     * @param lPassword lcPassword
     * @param onSuccess success callback
     * @param onError error callback
     */
    fun loginIM(
        lUsername: String,
        lPassword: String,
        onSuccess: (LCUser) -> Unit,
        onError: (String) -> Unit
    ) {
        LCUser.logIn(lUsername, lPassword).subscribe(DefaultObserver<LCUser>(onSuccess = {
            val lcIMClient: LCIMClient = LCIMClient.getInstance(it)
            lcIMClient.open(object : LCIMClientCallback() {
                override fun done(avimClient: LCIMClient, e: LCIMException?) {
                    client = avimClient
                    user = it
                    onSuccess.invoke(it)
                }
            })
        }, onError))
    }

    /**
     * 创建对话
     * @param uid objectId of target user
     * @param onSuccess success callback
     * @param onError error callback
     */
    fun findExistConversationOrCreate(
        uid: String,
        onSuccess: (LCIMConversation) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        getIMClient()?.createConversation(
            mutableListOf(uid), "${getCntUser()?.username ?: ""} & $uid", null, false, true,
            object : LCIMConversationCreatedCallback() {
                override fun done(conversation: LCIMConversation, e: LCIMException?) {
                    if (e == null) {
                        onSuccess(conversation)
                    } else {
                        onError(e.message.toString())
                    }
                }
            })
    }

    /**
     * 发送消息
     * @param conversation the LcConversation after login
     * @param text text string you want to send
     * @param onSuccess success callback
     * @param onError error callback
     */
    fun sendMsg(
        conversation: LCIMConversation,
        text: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val msg = LCIMTextMessage()
        msg.text = text
        conversation.sendMessage(msg, object : LCIMConversationCallback() {
            override fun done(e: LCIMException?) {
                if (e == null) {
                    onSuccess()
                } else {
                    onError(e.message.toString())
                }
            }
        })
    }

    /**
     * 查询和自己相关的所有对话
     * @param onSuccess success callback
     * @param onError error callback
     */
    fun queryConversation(onSuccess: (List<LCIMConversation>) -> Unit, onError: (String) -> Unit) {
        val query = getIMClient()?.conversationsQuery
        query?.whereContainedIn("m", mutableListOf(getCntUser()?.objectId ?: ""))
        query?.findInBackground(object : LCIMConversationQueryCallback() {
            override fun done(convs: List<LCIMConversation>, e: LCIMException?) {
                if (e == null) {
                    onSuccess(convs)
                } else {
                    onError(e.message.toString())
                }
            }
        })
    }

    /**
     * 从新到旧，查询历史消息
     * @param conv LCIMConversation
     * @param count the count of messages you want to query
     * @param onSuccess success callback
     * @param onError error callback
     */
    fun queryHistoryMessage(
        conv: LCIMConversation,
        count: Int = 20,
        onSuccess: (List<LCIMMessage>) -> Unit,
        onError: (String) -> Unit
    ) {
        conv.queryMessages(count, object : LCIMMessagesQueryCallback() {
            override fun done(messages: List<LCIMMessage>, e: LCIMException?) {
                if (e == null) {
                    onSuccess(messages)
                } else {
                    onError(e.message.toString())
                }
            }
        })
    }

    /**
     * 组合查询，查询所有满足条件的item
     * @param array the condition of query array
     * @param onSuccess success callback
     * @param onError error callback
     */
    fun queryContainsUsers(
        array: List<String>,
        onSuccess: (List<LCObject>) -> Unit,
        onError: (String) -> Unit
    ) {
        val queryList = arrayListOf<LCQuery<LCObject>>()
        array.forEach {
            val query = LCQuery<LCObject>("_User")
            query.whereEqualTo("objectId", it)
            queryList.add(query)
        }
        val queryPacket = LCQuery.or(queryList)
        queryPacket.findInBackground().subscribe(DefaultObserver<List<LCObject>>(onSuccess, onError))
    }

    class DefaultObserver<T : Any>(
        private val onSuccess: ((T) -> Unit)? = null,
        private val onError: ((String) -> Unit)? = null
    ) : Observer<T> {
        override fun onSubscribe(d: Disposable) {
        }

        override fun onError(e: Throwable) {
            onError?.invoke(e.message.toString())
        }

        override fun onComplete() {
        }

        override fun onNext(t: T) {
            onSuccess?.invoke(t)
        }

    }
}