package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.Cells.TextCheckCell;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellSelectBox;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;

// ★魔改(奶龙客户端): 顶层独立页 —— 下载速度 (20/35/50 倍并发下载加速)
@SuppressLint("RtlHardcoded")
public class NekoDownloadSpeedSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerSpeed = cellGroup.appendCell(new ConfigCellHeader("下载速度"));
    private final AbstractConfigCell speedBoostRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NekoConfig.downloadSpeedBoost,
            new String[]{
                    "关闭 (默认)",
                    "20 倍速",
                    "35 倍速",
                    "50 倍速",
            }, null));
    private final AbstractConfigCell dividerSpeed = cellGroup.appendCell(new ConfigCellDivider());

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
            if (row instanceof ConfigCellSelectBox) {
                ((ConfigCellSelectBox) row).onClick(view1);
            } else if (row instanceof ConfigCellTextCheck) {
                ((ConfigCellTextCheck) row).onClick((TextCheckCell) view1);
            }
        });

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
