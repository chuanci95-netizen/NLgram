package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;

import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellSelectBox;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;
import xyz.nextalone.nagram.NaConfig;

// ★魔改(NLgram): 从"通用"抽出的顶层独立页 —— AI工具
@SuppressLint("RtlHardcoded")
public class NekoAiSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerAiTools = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.PremiumPreviewAIEditor)));
    private final AbstractConfigCell summarizeTextButtonRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getSummarizeTextButton(),
            new String[]{
                    LocaleController.getString(R.string.Default),
                    LocaleController.getString(R.string.SummarizeTextButtonDisable),
                    LocaleController.getString(R.string.SummarizeTextButtonAlways),
            }, null));
    private final AbstractConfigCell disableAiEditorRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getDisableAiEditor()));
    private final AbstractConfigCell dividerAiTools = cellGroup.appendCell(new ConfigCellDivider());

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.PremiumPreviewAIEditor);
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
