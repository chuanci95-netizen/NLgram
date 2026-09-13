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
import xyz.nextalone.nagram.NaConfig;

// ★魔改(奶龙客户端): 从"通用"抽出的顶层独立页 —— 头像
@SuppressLint("RtlHardcoded")
public class NekoAvatarSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerAvatar = cellGroup.appendCell(new ConfigCellHeader("头像"));
    private final AbstractConfigCell showSquareAvatarRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getShowSquareAvatar()));
    private final AbstractConfigCell disableProfileAvatarBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getDisableProfileAvatarBlur()));
    private final AbstractConfigCell disableGooeyAvatarAnimationRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getDisableGooeyAvatarAnimation()));
    private final AbstractConfigCell hidePhoneRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.hidePhone));
    private final AbstractConfigCell dividerAvatar = cellGroup.appendCell(new ConfigCellDivider());

    @Override
    public String getTitle() {
        return "头像";
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
