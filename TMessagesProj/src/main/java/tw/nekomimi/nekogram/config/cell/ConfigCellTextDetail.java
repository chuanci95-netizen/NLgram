package tw.nekomimi.nekogram.config.cell;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.RecyclerListView;

import cn.hutool.core.util.StrUtil;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.ConfigItem;

public class ConfigCellTextDetail extends AbstractConfigCell {
    private final ConfigItem bindConfig;
    private final String title;
    private final String hint;
    private final boolean statusOnly; // ★奶龙客户端: true=详情只显示状态文案, 不暴露原始路径
    public final RecyclerListView.OnItemClickListener onItemClickListener;

    public ConfigCellTextDetail(ConfigItem bind, RecyclerListView.OnItemClickListener onItemClickListener, String hint) {
        this.bindConfig = bind;
        this.title = LocaleController.getString(bindConfig.getKey());
        this.hint = hint == null ? "" : hint;
        this.onItemClickListener = onItemClickListener;
        this.statusOnly = false;
    }

    // ★奶龙客户端: 自定义标题 + 状态文案(不显示原始值, 适合文件路径这种)
    public ConfigCellTextDetail(ConfigItem bind, RecyclerListView.OnItemClickListener onItemClickListener, String hint, String customTitle) {
        this.bindConfig = bind;
        this.title = customTitle;
        this.hint = hint == null ? "" : hint;
        this.onItemClickListener = onItemClickListener;
        this.statusOnly = true;
    }

    public int getType() {
        return CellGroup.ITEM_TYPE_TEXT_DETAIL;
    }

    public ConfigItem getBindConfig() {
        return bindConfig;
    }

    public String getKey() {
        return bindConfig == null ? null : bindConfig.getKey();
    }

    public boolean isEnabled() {
        return false;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder holder) {
        TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
        String detail;
        if (statusOnly) {
            detail = StrUtil.isNotBlank(bindConfig.String()) ? "已设置 · 点击更换 · 长按清除" : hint;
        } else {
            detail = StrUtil.isNotBlank(bindConfig.String()) ? bindConfig.String() : hint;
        }
        cell.setTextAndValue(title, detail, cellGroup.needSetDivider(this));
    }
}
