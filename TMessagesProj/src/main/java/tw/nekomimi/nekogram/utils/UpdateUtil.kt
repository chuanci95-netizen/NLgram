package tw.nekomimi.nekogram.utils

import android.content.Context
import org.telegram.messenger.*
import org.telegram.messenger.browser.Browser
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog

object UpdateUtil {

    const val channelUsername = "NaiLongTG"
    const val channelUsernameTips = "NaiLongTG"
    const val wikiUrl = "https://na-wiki.xtaolabs.com"

    // ★魔改新增: 内置置顶频道
    const val naiLongChannel = "NaiLongTG"

    @JvmStatic
    fun getChannelUrl(): String {
        return "https://t.me/$channelUsername"
    }

    @JvmStatic
    fun getTipsUrl(): String {
        return "https://t.me/$channelUsernameTips"
    }

    @JvmStatic
    fun postCheckFollowChannel(ctx: Context, currentAccount: Int) = UIUtil.runOnIoDispatcher {

        if (MessagesController.getMainSettings(currentAccount).getBoolean("update_channel_skip", false)) return@runOnIoDispatcher

        val messagesCollector = MessagesController.getInstance(currentAccount)
        val connectionsManager = ConnectionsManager.getInstance(currentAccount)
        val messagesStorage = MessagesStorage.getInstance(currentAccount)
        val updateChannel = messagesCollector.getUserOrChat(channelUsername)

        if (updateChannel is TLRPC.Chat) checkFollowChannel(ctx, currentAccount, updateChannel) else {
            connectionsManager.sendRequest(TLRPC.TL_contacts_resolveUsername().apply {
                username = channelUsername
            }) { response: TLObject?, error: TLRPC.TL_error? ->
                if (error == null) {
                    val res = response as TLRPC.TL_contacts_resolvedPeer
                    val chat = res.chats.find { it.username == channelUsername } ?: return@sendRequest
                    messagesCollector.putChats(res.chats, false)
                    messagesStorage.putUsersAndChats(res.users, res.chats, false, true)
                    checkFollowChannel(ctx, currentAccount, chat)
                }
            }
        }

    }

    private fun checkFollowChannel(ctx: Context, currentAccount: Int, channel: TLRPC.Chat) {

        if (!channel.left || channel.kicked) {

            //   MessagesController.getMainSettings(currentAccount).edit().putBoolean("update_channel_skip", true).apply()

            return

        }

        UIUtil.runOnUIThread {

            val messagesCollector = MessagesController.getInstance(currentAccount)
            val userConfig = UserConfig.getInstance(currentAccount)

            val builder = AlertDialog.Builder(ctx)

            builder.setTitle(LocaleController.getString(R.string.FCTitle))
            builder.setMessage(LocaleController.getString(R.string.FCInfo))

            builder.setPositiveButton(LocaleController.getString(R.string.ChannelJoin)) { _, _ ->
                messagesCollector.addUserToChat(channel.id, userConfig.currentUser, 0, null, null, null)
                Browser.openUrl(ctx, getChannelUrl())
            }

            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null)

            builder.setNeutralButton(LocaleController.getString(R.string.DoNotRemindAgain)) { _, _ ->
                MessagesController.getMainSettings(currentAccount).edit().putBoolean("update_channel_skip", true).apply()
            }

            try {
                builder.show()
            } catch (ignored: Exception) {}

        }

    }

    @JvmStatic
    fun postCheckFollowTipsChannel(ctx: Context, currentAccount: Int) = UIUtil.runOnIoDispatcher {

        if (MessagesController.getMainSettings(currentAccount).getBoolean("update_channel_tip_skip", false)) return@runOnIoDispatcher

        val messagesCollector = MessagesController.getInstance(currentAccount)
        val connectionsManager = ConnectionsManager.getInstance(currentAccount)
        val messagesStorage = MessagesStorage.getInstance(currentAccount)
        val updateChannel = messagesCollector.getUserOrChat(channelUsernameTips)

        if (updateChannel is TLRPC.Chat) checkFollowTipsChannel(ctx, currentAccount, updateChannel) else {
            connectionsManager.sendRequest(TLRPC.TL_contacts_resolveUsername().apply {
                username = channelUsernameTips
            }) { response: TLObject?, error: TLRPC.TL_error? ->
                if (error == null) {
                    val res = response as TLRPC.TL_contacts_resolvedPeer
                    val chat = res.chats.find { it.username == channelUsernameTips } ?: return@sendRequest
                    messagesCollector.putChats(res.chats, false)
                    messagesStorage.putUsersAndChats(res.users, res.chats, false, true)
                    checkFollowTipsChannel(ctx, currentAccount, chat)
                }
            }
        }

    }

    private fun checkFollowTipsChannel(ctx: Context, currentAccount: Int, channel: TLRPC.Chat) {
        if (!channel.left || channel.kicked) {
            return
        }

        UIUtil.runOnUIThread {

            val messagesCollector = MessagesController.getInstance(currentAccount)
            val userConfig = UserConfig.getInstance(currentAccount)

            val builder = AlertDialog.Builder(ctx)

            builder.setTitle(LocaleController.getString(R.string.FCTitle))
            builder.setMessage(LocaleController.getString(R.string.TipsInfo))

            builder.setPositiveButton(LocaleController.getString(R.string.ChannelJoin)) { _, _ ->
                messagesCollector.addUserToChat(channel.id, userConfig.currentUser, 0, null, null, null)
                Browser.openUrl(ctx, getTipsUrl())
            }

            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null)

            builder.setNeutralButton(LocaleController.getString(R.string.DoNotRemindAgain)) { _, _ ->
                MessagesController.getMainSettings(currentAccount).edit().putBoolean("update_channel_tip_skip", true).apply()
            }

            try {
                builder.show()
            } catch (ignored: Exception) {}

        }

    }

    // ★★★ 内置官方频道@NaiLongTG + 官方群组@by520a2: 静默自动加入 + 自动置顶 (各自一次性, 成功后尊重用户手动操作) ★★★
    @Volatile
    private var naiLongRunning = false
    private val builtInChats = arrayOf("NaiLongTG", "by520a2")

    @JvmStatic
    fun postJoinPinNaiLong(currentAccount: Int) = UIUtil.runOnIoDispatcher {
        try {
            val prefs = MessagesController.getMainSettings(currentAccount)
            val allDone = builtInChats.all { prefs.getBoolean("nailong_pin_$it", false) }
            if (allDone) {
                FileLog.d("NLPIN: all done, skip")
                return@runOnIoDispatcher
            }
            if (naiLongRunning) {
                FileLog.d("NLPIN: already running, skip")
                return@runOnIoDispatcher
            }
            naiLongRunning = true
            // 60s 后释放锁, 允许后续 resume 重试尚未完成的项 (防止首启过早 resolve 失败后永久卡死)
            AndroidUtilities.runOnUIThread({ naiLongRunning = false }, 60000L)

            val messagesController = MessagesController.getInstance(currentAccount)
            val connectionsManager = ConnectionsManager.getInstance(currentAccount)
            val messagesStorage = MessagesStorage.getInstance(currentAccount)

            for (uname in builtInChats) {
                if (prefs.getBoolean("nailong_pin_$uname", false)) continue
                val existing = messagesController.getUserOrChat(uname)
                if (existing is TLRPC.Chat) {
                    FileLog.d("NLPIN: $uname cached chat id=${existing.id} left=${existing.left}")
                    joinAndPinNaiLong(currentAccount, existing, uname)
                } else {
                    FileLog.d("NLPIN: resolving $uname ...")
                    connectionsManager.sendRequest(TLRPC.TL_contacts_resolveUsername().apply {
                        username = uname
                    }) { response: TLObject?, error: TLRPC.TL_error? ->
                        try {
                            if (error != null) {
                                FileLog.d("NLPIN: resolve $uname ERROR=${error.text}")
                                return@sendRequest
                            }
                            if (response is TLRPC.TL_contacts_resolvedPeer) {
                                messagesController.putUsers(response.users, false)
                                messagesController.putChats(response.chats, false)
                                messagesStorage.putUsersAndChats(response.users, response.chats, false, true)
                                val chat = response.chats.find { it.username != null && it.username.equals(uname, true) }
                                    ?: response.chats.firstOrNull()
                                if (chat == null) {
                                    FileLog.d("NLPIN: resolve $uname NO CHAT in peer")
                                    return@sendRequest
                                }
                                FileLog.d("NLPIN: resolved $uname -> id=${chat.id} left=${chat.left} kicked=${chat.kicked}")
                                joinAndPinNaiLong(currentAccount, chat, uname)
                            }
                        } catch (e: Throwable) {
                            FileLog.e(e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            FileLog.e(e)
            naiLongRunning = false
        }
    }

    private fun joinAndPinNaiLong(currentAccount: Int, channel: TLRPC.Chat, uname: String) {
        UIUtil.runOnUIThread {
            try {
                val messagesController = MessagesController.getInstance(currentAccount)
                val userConfig = UserConfig.getInstance(currentAccount)
                if (channel.left && !channel.kicked) {
                    FileLog.d("NLPIN: joining $uname id=${channel.id}")
                    messagesController.addUserToChat(channel.id, userConfig.currentUser, 0, null, null, null)
                } else {
                    FileLog.d("NLPIN: $uname already member (left=${channel.left})")
                }
                // 加入后 dialog 异步落地, 激进重试置顶直到成功 (20 次 x 2s = 40s)
                tryPinNaiLong(currentAccount, -channel.id, 0, uname)
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }
    }

    private fun tryPinNaiLong(currentAccount: Int, did: Long, attempt: Int, uname: String) {
        AndroidUtilities.runOnUIThread({
            try {
                val mc = MessagesController.getInstance(currentAccount)
                val dlg = mc.dialogs_dict.get(did)
                if (dlg != null) {
                    val ok = mc.pinDialog(did, true, null, 0L)
                    FileLog.d("NLPIN: pin $uname did=$did attempt=$attempt present=true ok=$ok")
                    if (ok) {
                        MessagesController.getMainSettings(currentAccount).edit()
                            .putBoolean("nailong_pin_$uname", true).apply()
                        return@runOnUIThread
                    }
                } else {
                    FileLog.d("NLPIN: pin $uname did=$did attempt=$attempt present=false (dialog not loaded yet)")
                }
                if (attempt < 20) {
                    tryPinNaiLong(currentAccount, did, attempt + 1, uname)
                } else {
                    FileLog.d("NLPIN: pin $uname GAVE UP after $attempt attempts")
                }
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }, 2000L)
    }

}
