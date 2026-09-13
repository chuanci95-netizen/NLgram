package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.Cells.TextCheckCell;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.ConfigItem;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;

// ★魔改(奶龙客户端): 顶层独立页 —— 下载速度(倍速开关单选) + 液态玻璃开关
@SuppressLint("RtlHardcoded")
public class NekoDownloadSpeedSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    // 下载加速倍速(单选: 开一个自动关其他)
    private final AbstractConfigCell headerSpeed = cellGroup.appendCell(new ConfigCellHeader("下载速度"));
    private final AbstractConfigCell boost4xRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.downloadBoost4x, null, "4 倍加速"));
    private final AbstractConfigCell boost12xRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.downloadBoost12x, null, "12 倍加速"));
    private final AbstractConfigCell boost24xRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.downloadBoost24x, null, "24 倍加速"));
    private final AbstractConfigCell boostMaxRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.downloadBoostMax, null, "极限"));
    private final AbstractConfigCell dividerSpeed = cellGroup.appendCell(new ConfigCellDivider());

    private final ConfigItem[] boosts = {
            NekoConfig.downloadBoost4x, NekoConfig.downloadBoost12x,
            NekoConfig.downloadBoost24x, NekoConfig.downloadBoostMax
    };

    @Override
    public String getTitle() {
        return "下载速度";
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        cellGroup.setListAdapter(listView, listAdapter);

        listView.setOnItemClickListener((view1, position) -> {
            AbstractConfigCell row = cellGroup.rows.get(position);
            if (row instanceof ConfigCellTextCheck) {
                ((ConfigCellTextCheck) row).onClick((TextCheckCell) view1);
            }
        });

        // ★单选: 打开一个倍速开关时, 自动关闭其他倍速开关
        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            for (ConfigItem b : boosts) {
                if (b.getKey().equals(key)) {
                    if (Boolean.TRUE.equals(newValue)) {
                        for (ConfigItem other : boosts) {
                            if (other != b && other.Bool()) other.setConfigBool(false);
                        }
                        if (listAdapter != null) listAdapter.notifyDataSetChanged();
                    }
                    break;
                }
            }
        };

        addRowsToMap();
        return view;
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            AbstractConfigCell row = cellGroup.rows.get(position);
            row.onBindViewHolder(holder);
        }
    }
}
