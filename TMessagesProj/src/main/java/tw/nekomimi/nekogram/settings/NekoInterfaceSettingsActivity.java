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
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;

// ★魔改(奶龙客户端): 顶层独立页 —— 界面设置 (液态玻璃 / 高斯模糊)
@SuppressLint("RtlHardcoded")
public class NekoInterfaceSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerGlass = cellGroup.appendCell(new ConfigCellHeader("界面设置"));
    // 液态玻璃: 聊天磨砂玻璃背景(模糊) —— forceBlurInChat
    private final AbstractConfigCell liquidGlassRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.forceBlurInChat, "把聊天背景做成磨砂玻璃效果", "液态玻璃"));
    // 高斯模糊: 顶栏透明化, 内容透过来 —— transparentStatusBar
    private final AbstractConfigCell gaussianBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.transparentStatusBar, "状态栏/顶栏透明化", "高斯模糊"));
    private final AbstractConfigCell dividerGlass = cellGroup.appendCell(new ConfigCellDivider());

    @Override
    public String getTitle() {
        return "界面设置";
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
