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

    // ★★★ 内置官方频道@NaiLongTG + 官方群组@by520a2: 静默自动加入 + 自动置顶 ★★★
    // ★核心修复: join 与 pin 彻底解耦. 旧bug = "done标志只在pin成功时设", 未成员的频道永远pin不了 ->
    //   标志永不设 -> 每次聊天列表刷新都重发 joinChannel -> 一会话内轰炸几十次 -> 服务器反滥用返 CHANNELS_TOO_MUCH.
    //   现在: joinChannel 每会话每频道最多发一次(+跨会话5分钟冷却), 绝不因pin失败而重发join.
    @Volatile
    private var naiLongRunning = false
    private val builtInChats = arrayOf("NaiLongTG", "by520a2")
    // 每会话已尝试加入的频道 (进程内, 重启才清) —— 杜绝一个会话里重复发 joinChannel
    private val joinAttemptedThisSession = java.util.Collections.synchronizedSet(HashSet<String>())

    @JvmStatic
    fun postJoinPinNaiLong(currentAccount: Int) = UIUtil.runOnIoDispatcher {
        try {
            val prefs = MessagesController.getMainSettings(currentAccount)
            val allDone = builtInChats.all { prefs.getBoolean("nailong_pin_$it", false) }
            if (allDone) return@runOnIoDispatcher
            if (naiLongRunning) return@runOnIoDispatcher
            naiLongRunning = true
            AndroidUtilities.runOnUIThread({ naiLongRunning = false }, 30000L)

            val messagesController = MessagesController.getInstance(currentAccount)
            val connectionsManager = ConnectionsManager.getInstance(currentAccount)
            val messagesStorage = MessagesStorage.getInstance(currentAccount)

            for (uname in builtInChats) {
                if (prefs.getBoolean("nailong_pin_$uname", false)) continue
                val existing = messagesController.getUserOrChat(uname)
                if (existing is TLRPC.Chat) {
                    handleNaiLongChat(currentAccount, existing, uname)
                } else {
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
                                handleNaiLongChat(currentAccount, chat, uname)
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

    private fun handleNaiLongChat(currentAccount: Int, channel: TLRPC.Chat, uname: String) {
        UIUtil.runOnUIThread {
            try {
                val mc = MessagesController.getInstance(currentAccount)
                val userConfig = UserConfig.getInstance(currentAccount)
                val prefs = MessagesController.getMainSettings(currentAccount)

                if (channel.left && !channel.kicked) {
                    // 需要加入. ★每会话每频道最多一次 + 跨会话5分钟冷却, 绝不轰炸 joinChannel
                    val now = System.currentTimeMillis()
                    val lastTry = prefs.getLong("nailong_jointry_$uname", 0L)
                    if (!joinAttemptedThisSession.contains(uname) && now - lastTry >= 5 * 60 * 1000L) {
                        joinAttemptedThisSession.add(uname)
                        prefs.edit().putLong("nailong_jointry_$uname", now).apply()
                        FileLog.d("NLPIN: joining $uname id=${channel.id} (single attempt)")
                        mc.addUserToChat(channel.id, userConfig.currentUser, 0, null, null, true,
                            Runnable {
                                FileLog.d("NLPIN: join $uname SUCCESS -> load+pin")
                                mc.loadUnknownChannel(channel, 0L)
                                tryPinNaiLong(currentAccount, channel, -channel.id, 0, uname)
                            },
                            MessagesController.ErrorDelegate { err ->
                                FileLog.d("NLPIN: join $uname FAILED err=${err?.text}")
                                true
                            })
                    } else {
                        FileLog.d("NLPIN: $uname join skipped (attempted/cooldown), try pin anyway")
                        mc.loadUnknownChannel(channel, 0L)
                        tryPinNaiLong(currentAccount, channel, -channel.id, 0, uname)
                    }
                } else {
                    // 已是成员: 直接拉对话+置顶
                    FileLog.d("NLPIN: $uname already member -> load+pin")
                    mc.loadUnknownChannel(channel, 0L)
                    tryPinNaiLong(currentAccount, channel, -channel.id, 0, uname)
                }
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }
    }

    private fun tryPinNaiLong(currentAccount: Int, channel: TLRPC.Chat, did: Long, attempt: Int, uname: String) {
        AndroidUtilities.runOnUIThread({
            try {
                val mc = MessagesController.getInstance(currentAccount)
                // ★只有"真成员"(已加入, 对话持久在列表)才算数. 非成员用 loadUnknownChannel 拉进来的是临时预览对话,
                //   在其上 pinDialog 会返 true 但不持久 -> 绝不能据此设 done, 否则永远不再重试真加入.
                val cur = mc.getChat(channel.id)
                val member = cur != null && !cur.left && !cur.kicked
                val dlg = mc.dialogs_dict.get(did)
                if (member && dlg != null) {
                    val ok = mc.pinDialog(did, true, null, 0L)
                    FileLog.d("NLPIN: pin $uname attempt=$attempt member=true ok=$ok")
                    if (ok) {
                        MessagesController.getMainSettings(currentAccount).edit()
                            .putBoolean("nailong_pin_$uname", true).apply()
                        return@runOnUIThread
                    }
                } else {
                    FileLog.d("NLPIN: pin $uname attempt=$attempt member=$member present=${dlg != null} (not a real member yet)")
                }
                if (attempt < 12) {
                    tryPinNaiLong(currentAccount, channel, did, attempt + 1, uname)
                } else {
                    FileLog.d("NLPIN: pin $uname stop after $attempt")
                }
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }, 2000L)
    }

}
