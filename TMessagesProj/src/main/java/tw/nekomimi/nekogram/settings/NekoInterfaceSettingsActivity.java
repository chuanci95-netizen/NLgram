package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.config.CellGroup;
import tw.nekomimi.nekogram.config.cell.AbstractConfigCell;
import tw.nekomimi.nekogram.config.cell.ConfigCellCustom;
import tw.nekomimi.nekogram.config.cell.ConfigCellDivider;
import tw.nekomimi.nekogram.config.cell.ConfigCellHeader;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextCheck;
import tw.nekomimi.nekogram.config.cell.ConfigCellTextDetail;
import tw.nekomimi.nekogram.helpers.AppRestartHelper;

import xyz.nextalone.nagram.NaConfig;

// ★魔改(奶龙客户端): 顶层独立页 —— 界面设置(深度版液态玻璃 + 自定义背景图 + 界面透明)
@SuppressLint("RtlHardcoded")
public class NekoInterfaceSettingsActivity extends BaseNekoXSettingsActivity {

    private static final int NL_REQ_BG = 0x4E4C01; // 选背景图 requestCode
    private static final int SB_INTENSITY = 0, SB_ANGLE = 1, SB_ALPHA = 2;

    private final CellGroup a = cellGroup = new CellGroup(this);

    // —— 液态玻璃 ——
    private final AbstractConfigCell headerGlass = cellGroup.appendCell(new ConfigCellHeader("液态玻璃"));
    private final AbstractConfigCell liquidGlassRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.liquidGlassEnable, "iOS风格透明玻璃质感 · 顶栏/底栏/面板通透折射 · 重进聊天生效", "液态玻璃"));
    private final AbstractConfigCell glassIntensityRow = cellGroup.appendCell(new ConfigCellCustom("LiquidGlassIntensity", ConfigCellCustom.CUSTOM_ITEM_LiquidGlassIntensity, true));
    private final AbstractConfigCell glassAngleRow = cellGroup.appendCell(new ConfigCellCustom("LiquidGlassAngle", ConfigCellCustom.CUSTOM_ITEM_LiquidGlassAngle, true));
    private final AbstractConfigCell dividerGlass = cellGroup.appendCell(new ConfigCellDivider());

    // —— 模糊效果 ——
    private final AbstractConfigCell headerBlur = cellGroup.appendCell(new ConfigCellHeader("模糊效果"));
    private final AbstractConfigCell gaussianBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.gaussianBlurEnable, "聊天/面板背景毛玻璃模糊", "高斯模糊"));
    private final AbstractConfigCell dividerBlur = cellGroup.appendCell(new ConfigCellDivider());

    // —— 背景 & 界面透明 ——
    private final AbstractConfigCell headerTrans = cellGroup.appendCell(new ConfigCellHeader("背景 & 界面透明"));
    private final AbstractConfigCell backgroundImageRow = cellGroup.appendCell(new ConfigCellTextDetail(NekoConfig.customBackgroundImage, (view, position) -> pickBackgroundImage(), "点击从相册选择一张背景图片", "自定义背景图"));
    private final AbstractConfigCell transparentRow = cellGroup.appendCell(new ConfigCellTextCheck(NekoConfig.interfaceTransparent, "聊天列表/设置卡片/按钮全部镂空透明 · 露出背景图与玻璃", "界面透明"));
    private final AbstractConfigCell alphaRow = cellGroup.appendCell(new ConfigCellCustom("InterfaceTransparentAlpha", ConfigCellCustom.CUSTOM_ITEM_InterfaceAlpha, true));
    private final AbstractConfigCell dividerTrans = cellGroup.appendCell(new ConfigCellDivider());

    @Override
    public String getTitle() {
        return "界面设置";
    }

    @Override
    public View createView(Context context) {
        // 进页用 LiteMode 真值同步玻璃/模糊开关
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
            } else if (key.equals(NekoConfig.interfaceTransparent.getKey())) {
                if (on && NekoConfig.customBackgroundImage.String().isEmpty()) {
                    // 开了界面透明但还没设背景图 → 先去相册选一张(选完自动重启应用), 没图透明毫无意义还会露空白
                    pickBackgroundImage();
                } else {
                    restartToApply();
                }
            }
        };

        listView.setOnItemClickListener((view1, position, x, y) -> {
            AbstractConfigCell row = cellGroup.rows.get(position);
            if (row instanceof ConfigCellTextCheck) {
                ((ConfigCellTextCheck) row).onClick((TextCheckCell) view1);
            } else if (row instanceof ConfigCellTextDetail) {
                RecyclerListView.OnItemClickListener o = ((ConfigCellTextDetail) row).onItemClickListener;
                if (o != null) {
                    try {
                        o.onItemClick(view1, position);
                    } catch (Exception ignore) {
                    }
                }
            }
        });

        listView.setOnItemLongClickListener((view1, position) -> {
            AbstractConfigCell row = cellGroup.rows.get(position);
            if (row == backgroundImageRow) {
                clearBackgroundImage();
                return true;
            }
            return false;
        });

        addRowsToMap();
        return view;
    }

    // 玻璃/模糊切换: 弹重启提示 + 重建视图栈即时生效
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

    // 界面透明/背景图 变更: 更新激活标志 + 重启彻底应用(聊天列表等用缓存画笔的界面必须重启才透, live重建不彻底)
    private void restartToApply() {
        try {
            Theme.updateNlTransparentActive();
            AppRestartHelper.triggerRebirth();
        } catch (Exception ignore) {
        }
    }

    private void pickBackgroundImage() {
        if (getParentActivity() == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "选择背景图片"), NL_REQ_BG);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void clearBackgroundImage() {
        NekoConfig.customBackgroundImage.setConfigString("");
        restartToApply();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == NL_REQ_BG && resultCode == Activity.RESULT_OK && data != null && data.getData() != null && getParentActivity() != null) {
            InputStream in = null;
            FileOutputStream fos = null;
            try {
                Uri uri = data.getData();
                File out = new File(ApplicationLoader.getFilesDirFixed(), "nl_custom_bg.jpg");
                in = getParentActivity().getContentResolver().openInputStream(uri);
                fos = new FileOutputStream(out);
                byte[] buf = new byte[16384];
                int r;
                while ((r = in.read(buf)) > 0) {
                    fos.write(buf, 0, r);
                }
                fos.flush();
                NekoConfig.customBackgroundImage.setConfigString(out.getAbsolutePath());
                // 设了背景图但界面透明没开 → 自动开启, 否则背景图被不透明卡片盖住看不见
                if (!NekoConfig.interfaceTransparent.Bool()) {
                    NekoConfig.interfaceTransparent.setConfigBool(true);
                }
                restartToApply(); // 重启彻底应用, 修"换背景图没效果"
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignore) {}
                try { if (fos != null) fos.close(); } catch (Exception ignore) {}
            }
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
                return new NlSeekBar(mContext, SB_INTENSITY);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_LiquidGlassAngle) {
                return new NlSeekBar(mContext, SB_ANGLE);
            } else if (viewType == ConfigCellCustom.CUSTOM_ITEM_InterfaceAlpha) {
                return new NlSeekBar(mContext, SB_ALPHA);
            }
            return super.onCreateViewHolderView(viewType);
        }
    }

    // 通用滑块: 折射强度 / 折射角度 / 界面透明度
    private class NlSeekBar extends FrameLayout {
        private final SeekBarView bar;
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final int mode;

        NlSeekBar(Context context, int mode) {
            super(context);
            this.mode = mode;
            setWillNotDraw(false);
            textPaint.setTextSize(AndroidUtilities.dp(14));
            bar = new SeekBarView(context);
            bar.setReportChanges(true);
            bar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    if (mode == SB_ANGLE) {
                        NaConfig.INSTANCE.getLiquidGlassAngle().setConfigInt(Math.round(progress * 360f));
                    } else if (mode == SB_INTENSITY) {
                        NaConfig.INSTANCE.getLiquidGlassIntensity().setConfigInt(Math.round(progress * 150f));
                    } else {
                        NekoConfig.interfaceTransparentAlpha.setConfigInt(Math.round(progress * 100f));
                        if (stop) {
                            // 透明度实时预览: 全局重建界面(不重启), 卡片重新按新透明度绘制
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
                        }
                    }
                    invalidate();
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {
                }
            });
            addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 12, 23, 52, 5));
        }

        private String label() {
            if (mode == SB_ANGLE) return "折射角度";
            if (mode == SB_INTENSITY) return "折射强度";
            return "界面透明度";
        }

        private String valueText() {
            if (mode == SB_ANGLE) return NaConfig.INSTANCE.getLiquidGlassAngle().Int() + "°";
            if (mode == SB_INTENSITY) return NaConfig.INSTANCE.getLiquidGlassIntensity().Int() + "%";
            return NekoConfig.interfaceTransparentAlpha.Int() + "%";
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            canvas.drawText(label(), AndroidUtilities.dp(21), AndroidUtilities.dp(20), textPaint);
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            String value = valueText();
            canvas.drawText(value, getMeasuredWidth() - AndroidUtilities.dp(47), AndroidUtilities.dp(47), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(66), MeasureSpec.EXACTLY));
            float value;
            if (mode == SB_ANGLE) {
                value = NaConfig.INSTANCE.getLiquidGlassAngle().Int() / 360f;
            } else if (mode == SB_INTENSITY) {
                value = NaConfig.INSTANCE.getLiquidGlassIntensity().Int() / 150f;
            } else {
                value = NekoConfig.interfaceTransparentAlpha.Int() / 100f;
            }
            bar.setProgress(value);
        }
    }
}
