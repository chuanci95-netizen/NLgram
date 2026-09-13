package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.UndoView;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellCustom;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;

import xyz.nextalone.nagram.NaConfig;

// ★魔改(奶龙客户端): 顶层独立页 —— 界面设置(深度版液态玻璃)
// 液态玻璃开关直接驱动 fork 原生 LiteMode.FLAG_LIQUID_GLASS (iOS26 真·玻璃折射, 全 app 生效),
// 折射强度/角度滑块调 NaConfig, 高斯模糊驱动 LiteMode.FLAG_CHAT_BLUR。默认关, 想开自己来这点。
@SuppressLint("RtlHardcoded")
public class NekoInterfaceSettingsActivity extends BaseNekoXSettingsActivity {

    private final CellGroup a = cellGroup = new CellGroup(this);

    private final AbstractConfigCell headerGlass = cellGroup.appendCell(new ConfigCellHeader("液态玻璃"));
    // 液态玻璃主开关 → LiteMode.FLAG_LIQUID_GLASS
    private final AbstractConfigCell liquidGlassRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.liquidGlassEnable, "iOS风格透明玻璃质感 · 顶栏/底栏/面板通透折射 · 重进聊天生效", "液态玻璃"));
    // 折射强度 / 折射角度 滑块 (仅安卓13+ 生效)
    private final AbstractConfigCell glassIntensityRow = cellGroup.appendCell(new ConfigCellCustom("LiquidGlassIntensity", ConfigCellCustom.CUSTOM_ITEM_LiquidGlassIntensity, true));
    private final AbstractConfigCell glassAngleRow = cellGroup.appendCell(new ConfigCellCustom("LiquidGlassAngle", ConfigCellCustom.CUSTOM_ITEM_LiquidGlassAngle, true));
    private final AbstractConfigCell dividerGlass = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerBlur = cellGroup.appendCell(new ConfigCellHeader("模糊效果"));
    // 高斯模糊 → LiteMode.FLAG_CHAT_BLUR
    private final AbstractConfigCell gaussianBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.gaussianBlurEnable, "聊天/面板背景毛玻璃模糊", "高斯模糊"));
    private final AbstractConfigCell dividerBlur = cellGroup.appendCell(new ConfigCellDivider());

    @Override
    public String getTitle() {
        return "界面设置";
    }

    @Override
    public View createView(Context context) {
        // 每次进页, 用 LiteMode 真值同步开关 UI (可能在别处被改过)
        NekoConfig.liquidGlassEnable.setConfigBool(LiteMode.isEnabledSetting(LiteMode.FLAG_LIQUID_GLASS));
        NekoConfig.gaussianBlurEnable.setConfigBool(LiteMode.isEnabledSetting(LiteMode.FLAG_CHAT_BLUR));

        View view = super.createView(context);

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        cellGroup.setListAdapter(listView, listAdapter);

        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            boolean on = (newValue instanceof Boolean) && (Boolean) newValue;
            if (key.equals(NekoConfig.liquidGlassEnable.getKey())) {
                LiteMode.toggleFlag(LiteMode.FLAG_LIQUID_GLASS, on);
                applyGlassLive();
            } else if (key.equals(NekoConfig.gaussianBlurEnable.getKey())) {
                LiteMode.toggleFlag(LiteMode.FLAG_CHAT_BLUR, on);
                applyGlassLive();
            }
        };

        listView.setOnItemClickListener((view1, position) -> {
            AbstractConfigCell row = cellGroup.rows.get(position);
            if (row instanceof ConfigCellTextCheck) {
                ((ConfigCellTextCheck) row).onClick((TextCheckCell) view1);
            }
        });

        addRowsToMap();
        return view;
    }

    // 切换后: 弹「需重启」提示 + 重建当前视图栈, 让玻璃/模糊尽量即时生效
    private void applyGlassLive() {
        try {
            if (tooltip != null) {
                tooltip.showWithAction(0, UndoView.ACTION_NEED_RESATRT, null, null);
            }
            if (parentLayout != null) {
                parentLayout.rebuildAllFragmentViews(false, false);
            }
        } catch (Exception ignore) {
        }
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

        @Override
        public View onCreateViewHolderView(int viewType) {
            if (viewType == ConfigCellCustom.CUSTOM_ITEM_LiquidGlassIntensity) {
                return new LiquidGlassSeekBar(mContext, false);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_LiquidGlassAngle) {
                return new LiquidGlassSeekBar(mContext, true);
            }
            return super.onCreateViewHolderView(viewType);
        }
    }

    // 折射强度/角度 滑块 (照搬 fork 原实现, 标签中文化)
    private class LiquidGlassSeekBar extends FrameLayout {
        private final SeekBarView bar;
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final boolean angle;

        LiquidGlassSeekBar(Context context, boolean angle) {
            super(context);
            this.angle = angle;
            setWillNotDraw(false);
            textPaint.setTextSize(AndroidUtilities.dp(14));
            bar = new SeekBarView(context);
            bar.setReportChanges(true);
            bar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    if (angle) {
                        NaConfig.INSTANCE.getLiquidGlassAngle().setConfigInt(Math.round(progress * 360f));
                    } else {
                        NaConfig.INSTANCE.getLiquidGlassIntensity().setConfigInt(Math.round(progress * 150f));
                    }
                    invalidate();
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {
                }
            });
            addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 12, 23, 52, 5));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            canvas.drawText(angle ? "折射角度" : "折射强度", AndroidUtilities.dp(21), AndroidUtilities.dp(20), textPaint);
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            String value = angle ? NaConfig.INSTANCE.getLiquidGlassAngle().Int() + "°" : NaConfig.INSTANCE.getLiquidGlassIntensity().Int() + "%";
            canvas.drawText(value, getMeasuredWidth() - AndroidUtilities.dp(47), AndroidUtilities.dp(47), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(66), MeasureSpec.EXACTLY));
            float value = angle ? NaConfig.INSTANCE.getLiquidGlassAngle().Int() / 360f : NaConfig.INSTANCE.getLiquidGlassIntensity().Int() / 150f;
            bar.setProgress(value);
        }
    }
}
