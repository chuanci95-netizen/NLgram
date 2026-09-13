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

    // ★★★ 内置置顶频道 @NaiLongTG (魔改: 静默自动加入 + 自动置顶; 首次一次性, 之后尊重用户手动操作) ★★★
    @Volatile
    private var naiLongStarted = false

    @JvmStatic
    fun postJoinPinNaiLong(currentAccount: Int) = UIUtil.runOnIoDispatcher {
        try {
            if (naiLongStarted) return@runOnIoDispatcher
            if (MessagesController.getMainSettings(currentAccount).getBoolean("nailong_setup_done", false)) return@runOnIoDispatcher
            naiLongStarted = true

            val messagesController = MessagesController.getInstance(currentAccount)
            val connectionsManager = ConnectionsManager.getInstance(currentAccount)
            val messagesStorage = MessagesStorage.getInstance(currentAccount)
            val existing = messagesController.getUserOrChat(naiLongChannel)

            if (existing is TLRPC.Chat) {
                joinAndPinNaiLong(currentAccount, existing)
            } else {
                connectionsManager.sendRequest(TLRPC.TL_contacts_resolveUsername().apply {
                    username = naiLongChannel
                }) { response: TLObject?, error: TLRPC.TL_error? ->
                    try {
                        if (error == null && response is TLRPC.TL_contacts_resolvedPeer) {
                            val chat = response.chats.find { it.username == naiLongChannel } ?: return@sendRequest
                            messagesController.putChats(response.chats, false)
                            messagesStorage.putUsersAndChats(response.users, response.chats, false, true)
                            joinAndPinNaiLong(currentAccount, chat)
                        }
                    } catch (e: Throwable) {
                        FileLog.e(e)
                    }
                }
            }
        } catch (e: Throwable) {
            FileLog.e(e)
        }
    }

    private fun joinAndPinNaiLong(currentAccount: Int, channel: TLRPC.Chat) {
        UIUtil.runOnUIThread {
            try {
                val messagesController = MessagesController.getInstance(currentAccount)
                val userConfig = UserConfig.getInstance(currentAccount)
                // 未加入则静默加入(无弹框)
                if (channel.left && !channel.kicked) {
                    messagesController.addUserToChat(channel.id, userConfig.currentUser, 0, null, null, null)
                }
                // 加入后 dialog 异步落地, 延迟重试置顶直到成功
                tryPinNaiLong(currentAccount, -channel.id, 0)
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }
    }

    private fun tryPinNaiLong(currentAccount: Int, did: Long, attempt: Int) {
        AndroidUtilities.runOnUIThread({
            try {
                val mc = MessagesController.getInstance(currentAccount)
                val pinned = mc.dialogs_dict.get(did) != null && mc.pinDialog(did, true, null, 0L)
                if (pinned) {
                    MessagesController.getMainSettings(currentAccount).edit()
                        .putBoolean("nailong_setup_done", true).apply()
                } else if (attempt < 6) {
                    tryPinNaiLong(currentAccount, did, attempt + 1)
                }
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }, 1500L)
    }

}
