package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.RecyclerListView;

import cn.hutool.core.util.StrUtil;
import kotlin.Unit;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.NekoXConfig;
import tw.nekomimi.nekogram.config.ConfigItem;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellCustom;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellSelectBox;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextDetail;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextInput;
import tw.nekomimi.nekogram.transtale.Translator;
import tw.nekomimi.nekogram.transtale.TranslatorKt;
import tw.nekomimi.nekogram.ui.BottomBuilder;
import tw.nekomimi.nekogram.ui.PopupBuilder;
import xyz.nextalone.nagram.NaConfig;

// ★魔改(NLgram): 从"通用"抽出的顶层独立页 —— 翻译
@SuppressLint("RtlHardcoded")
public class NekoTranslationSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerTranslation = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString("Translate")));
    private final AbstractConfigCell translationProviderRow = cellGroup.appendCell(new ConfigCellCustom("TranslationProvider", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell useTelegramTranslateInChatRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.useTelegramTranslateInChat));
    private final AbstractConfigCell translateToLangRow = cellGroup.appendCell(new ConfigCellCustom("TranslateToLang", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell translateInputToLangRow = cellGroup.appendCell(new ConfigCellCustom("TranslateInputToLang", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell googleCloudTranslateKeyRow = cellGroup.appendCell(new ConfigCellTextDetail(NekoConfig.googleCloudTranslateKey, (view, position) -> {
        customDialog_BottomInputString(position, NekoConfig.googleCloudTranslateKey, LocaleController.getString("GoogleCloudTransKeyNotice"), "Key");
    }, LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty)));
    private final AbstractConfigCell deepLxCustomApiRow = cellGroup.appendCell(new ConfigCellTextInput(null, NaConfig.INSTANCE.getDeepLxCustomApi(), "", null));
    private final AbstractConfigCell deepLApiKeyRow = cellGroup.appendCell(new ConfigCellTextDetail(NaConfig.INSTANCE.getDeepLApiKey(), (view, position) -> {
        customDialog_BottomInputString(position, NaConfig.INSTANCE.getDeepLApiKey(), LocaleController.getString(R.string.DeepLApiKeyNotice), "Key");
    }, LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty)));
    private final AbstractConfigCell deepLFreeApiKeyRow = cellGroup.appendCell(new ConfigCellTextDetail(NaConfig.INSTANCE.getDeepLFreeApiKey(), (view, position) -> {
        customDialog_BottomInputString(position, NaConfig.INSTANCE.getDeepLFreeApiKey(), LocaleController.getString(R.string.DeepLFreeApiKeyNotice), "Key");
    }, LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty)));
    private final AbstractConfigCell deepLFormalityRow = cellGroup.appendCell(new ConfigCellSelectBox(null, NaConfig.INSTANCE.getDeepLFormality(),
            new String[]{
                    LocaleController.getString(R.string.DeepLFormalityDefault),
                    LocaleController.getString(R.string.DeepLFormalityMore),
                    LocaleController.getString(R.string.DeepLFormalityLess),
            }, null));
    private final AbstractConfigCell llmSettingsRow = cellGroup.appendCell(new ConfigCellCustom("LLMSettings", CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell hideOriginAfterTranslationRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getHideOriginAfterTranslation()));
    private final AbstractConfigCell autoTranslateRow = cellGroup.appendCell(new ConfigCellTextCheck(NaConfig.INSTANCE.getAutoTranslate(), LocaleController.getString("AutoTranslateAbout")));
    private final AbstractConfigCell dividerTranslation = cellGroup.appendCell(new ConfigCellDivider());

    @Override
    public String getTitle() {
        return LocaleController.getString("Translate", R.string.Translate);
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        setCanNotChange();
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        cellGroup.setListAdapter(listView, listAdapter);

        listView.setOnItemClickListener((view1, position) -> {
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellTextCheck) {
                ((ConfigCellTextCheck) a).onClick((TextCheckCell) view1);
            } else if (a instanceof ConfigCellSelectBox) {
                ((ConfigCellSelectBox) a).onClick(view1);
            } else if (a instanceof ConfigCellTextInput) {
                ((ConfigCellTextInput) a).onClick();
            } else if (a instanceof ConfigCellTextDetail) {
                RecyclerListView.OnItemClickListener o = ((ConfigCellTextDetail) a).onItemClickListener;
                if (o != null) {
                    try {
                        o.onItemClick(view1, position);
                    } catch (Exception e) {
                    }
                }
            } else if (a == translationProviderRow) {
                if (!((ConfigCellCustom) a).enabled) return;
                PopupBuilder builder = new PopupBuilder(view1);
                builder.setItems(new String[]{
                        LocaleController.getString(R.string.ProviderGoogleTranslate),
                        LocaleController.getString(R.string.ProviderGoogleTranslateCN),
                        LocaleController.getString(R.string.ProviderGoogleTranslate) + " 2",
                        LocaleController.getString(R.string.ProviderLingocloud),
                        LocaleController.getString(R.string.ProviderMicrosoftTranslator),
                        LocaleController.getString(R.string.ProviderVolcengineTranslate),
                        LocaleController.getString(R.string.ProviderDeepLxTranslate),
                        LocaleController.getString(R.string.ProviderTelegramAPI),
                        LocaleController.getString(R.string.ProviderTranSmartTranslate),
                        LocaleController.getString(R.string.ProviderLLMTranslate),
                        LocaleController.getString(R.string.ProviderDeepLTranslate),
                        LocaleController.getString(R.string.ProviderDeepLFreeTranslate),
                }, (i, __) -> {
                    NekoConfig.translationProvider.setConfigInt(i + 1);
                    updateRows();
                    listAdapter.notifyItemChanged(position);
                    return Unit.INSTANCE;
                });
                builder.show();
            } else if (a == translateToLangRow || a == translateInputToLangRow) {
                Translator.showTargetLangSelect(view1, a == translateInputToLangRow, (locale) -> {
                    if (a == translateToLangRow) {
                        NekoConfig.translateToLang.setConfigString(TranslatorKt.getLocale2code(locale));
                    } else {
                        NekoConfig.translateInputLang.setConfigString(TranslatorKt.getLocale2code(locale));
                    }
                    listAdapter.notifyItemChanged(position);
                    return Unit.INSTANCE;
                });
            } else if (a == llmSettingsRow) {
                presentFragment(new NekoLLMSettingsActivity());
            }
        });

        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            if (key.equals(NekoConfig.useTelegramTranslateInChat.getKey())) {
                RecyclerView.ViewHolder vh = listView.findViewHolderForAdapterPosition(cellGroup.rows.indexOf(translationProviderRow));
                TextSettingsCell cell = vh != null ? (TextSettingsCell) vh.itemView : null;
                if (NekoConfig.useTelegramTranslateInChat.Bool()) {
                    NekoConfig.translationProvider.setConfigInt(Translator.providerTelegram);
                    ((ConfigCellCustom) translationProviderRow).setEnabled(false);
                    if (cell != null) cell.setEnabled(false);
                } else {
                    ((ConfigCellCustom) translationProviderRow).setEnabled(true);
                    if (cell != null) cell.setEnabled(true);
                }
                updateRows();
                listAdapter.notifyItemChanged(cellGroup.rows.indexOf(translationProviderRow));
            }
        };

        addRowsToMap();
        return view;
    }

    @Override
    protected void setCanNotChange() {
        if (NekoConfig.useTelegramTranslateInChat.Bool())
            ((ConfigCellCustom) translationProviderRow).setEnabled(false);
        else
            ((ConfigCellCustom) translationProviderRow).setEnabled(true);

        // Control provider-specific config rows visibility
        boolean isLLMProvider = NekoConfig.translationProvider.Int() == Translator.providerLLM;
        boolean isDeepLProvider = NekoConfig.translationProvider.Int() == Translator.providerDeepL;
        boolean isDeepLOfficialProvider = NekoConfig.translationProvider.Int() == Translator.providerDeepLOfficial;
        boolean isDeepLFreeProvider = NekoConfig.translationProvider.Int() == Translator.providerDeepLFree;
        boolean isGoogleCloudProvider = NekoConfig.translationProvider.Int() == Translator.providerGoogle;

        cellGroup.rows.remove(llmSettingsRow);
        cellGroup.rows.remove(deepLxCustomApiRow);
        cellGroup.rows.remove(deepLApiKeyRow);
        cellGroup.rows.remove(deepLFreeApiKeyRow);
        cellGroup.rows.remove(deepLFormalityRow);
        cellGroup.rows.remove(googleCloudTranslateKeyRow);

        if (isLLMProvider) {
            int insertIndex = cellGroup.rows.indexOf(translateInputToLangRow) + 1;
            cellGroup.rows.add(insertIndex, llmSettingsRow);
        } else if (isDeepLProvider) {
            int insertIndex = cellGroup.rows.indexOf(translateInputToLangRow) + 1;
            cellGroup.rows.add(insertIndex, deepLxCustomApiRow);
            cellGroup.rows.add(insertIndex + 1, deepLFormalityRow);
        } else if (isDeepLOfficialProvider) {
            int insertIndex = cellGroup.rows.indexOf(translateInputToLangRow) + 1;
            cellGroup.rows.add(insertIndex, deepLApiKeyRow);
            cellGroup.rows.add(insertIndex + 1, deepLFormalityRow);
        } else if (isDeepLFreeProvider) {
            int insertIndex = cellGroup.rows.indexOf(translateInputToLangRow) + 1;
            cellGroup.rows.add(insertIndex, deepLFreeApiKeyRow);
            cellGroup.rows.add(insertIndex + 1, deepLFormalityRow);
        } else if (isGoogleCloudProvider) {
            int insertIndex = cellGroup.rows.indexOf(translateInputToLangRow) + 1;
            cellGroup.rows.add(insertIndex, googleCloudTranslateKeyRow);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            updateRows();
        }
    }

    private void customDialog_BottomInputString(int position, ConfigItem bind, String subtitle, String hint) {
        BottomBuilder builder = new BottomBuilder(getParentActivity());
        builder.addTitle(LocaleController.getString(bind.getKey()), subtitle);
        EditText keyField = builder.addEditText(hint);
        if (StrUtil.isNotBlank(bind.String())) {
            keyField.setText(bind.String());
        }
        builder.addCancelButton();
        builder.addOkButton((it) -> {
            String key = keyField.getText().toString();
            if (StrUtil.isBlank(key)) key = null;
            bind.setConfigString(key);
            listAdapter.notifyItemChanged(position);
            return Unit.INSTANCE;
        });
        builder.show();
        keyField.requestFocus();
        AndroidUtilities.showKeyboard(keyField);
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellCustom && holder.itemView instanceof TextSettingsCell) {
                TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                if (a == translationProviderRow) {
                    String value;
                    switch (NekoConfig.translationProvider.Int()) {
                        case Translator.providerGoogle:
                            value = LocaleController.getString(R.string.ProviderGoogleTranslate);
                            break;
                        case Translator.providerGoogleCN:
                            value = LocaleController.getString(R.string.ProviderGoogleTranslateCN);
                            break;
                        case Translator.providerGoogle2:
                            value = LocaleController.getString(R.string.ProviderGoogleTranslate) + " 2";
                            break;
                        case Translator.providerLingo:
                            value = LocaleController.getString(R.string.ProviderLingocloud);
                            break;
                        case Translator.providerMicrosoft:
                            value = LocaleController.getString(R.string.ProviderMicrosoftTranslator);
                            break;
                        case Translator.providerVolcengine:
                            value = LocaleController.getString(R.string.ProviderVolcengineTranslate);
                            break;
                        case Translator.providerDeepL:
                            value = LocaleController.getString(R.string.ProviderDeepLxTranslate);
                            break;
                        case Translator.providerTelegram:
                            value = LocaleController.getString(R.string.ProviderTelegramAPI);
                            break;
                        case Translator.providerTranSmart:
                            value = LocaleController.getString(R.string.ProviderTranSmartTranslate);
                            break;
                        case Translator.providerLLM:
                            value = LocaleController.getString(R.string.ProviderLLMTranslate);
                            break;
                        case Translator.providerDeepLOfficial:
                            value = LocaleController.getString(R.string.ProviderDeepLTranslate);
                            break;
                        case Translator.providerDeepLFree:
                            value = LocaleController.getString(R.string.ProviderDeepLFreeTranslate);
                            break;
                        default:
                            value = "Unknown";
                    }
                    textCell.setTextAndValue(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), value, divider);
                    textCell.setCanDisable(true);
                    if (NekoConfig.useTelegramTranslateInChat.Bool()) textCell.setEnabled(false);
                } else if (a == translateToLangRow) {
                    textCell.setTextAndValue(LocaleController.getString("TransToLang", R.string.TransToLang), NekoXConfig.formatLang(NekoConfig.translateToLang.String()), divider);
                } else if (a == translateInputToLangRow) {
                    textCell.setTextAndValue(LocaleController.getString("TransInputToLang", R.string.TransInputToLang), NekoXConfig.formatLang(NekoConfig.translateInputLang.String()), divider);
                } else if (a == llmSettingsRow) {
                    textCell.setTextAndValue(LocaleController.getString("LLMTranslatorSettings", R.string.LLMTranslatorSettings), "", divider);
                }
            } else {
                a.onBindViewHolder(holder);
            }
        }
    }
}
