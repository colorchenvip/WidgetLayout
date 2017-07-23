package com.rexy.widgets.group;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.rexy.widgetlayout.R;
import com.rexy.widgets.drawable.FloatDrawable;

/**
 * 1.支持容器内容自身的gravity,maxWidth,maxHeight.
 * 2.支持直接子 View 的 layout_gravity,maxWidth,maxHeight 等。
 * 3.支持水平和垂直视图的滑动计算 Api。
 * 5.支持容器四边的描边定制
 * 6.支持内容间隔的分割线定制和内容视图的间距。
 * 7.支持接口实现子视图接管当前容器的事件
 * 8.支持接口实现绘制 Item或自身 的装饰和 Offset。
 * 9.可监听当前视图可见区域的变法。
 * <p>
 * <p>
 * 实现子类需要重写dispatchMeasure和dispatchLayout 两个方法。
 * 其中dispatchMeasure来实现child 的测量，最终需要调用setContentSize 方法。
 * <p>
 * 支持Hover Drawable 动画渐变,类似ios 效果.
 * <p>
 * <declare-styleable name="BaseViewGroup">
 * <!--hover drawable 忽略手势滑动到自身之外取消按下状态-->
 * <attr name="ignoreForegroundStateWhenTouchOut" format="boolean"/>
 * <!--hover drawable 颜色-->
 * <attr name="foregroundColor" format="color"/>
 * <!--hover drawable  圆角-->
 * <attr name="foregroundRadius" format="dimension"/>
 * <!--hover drawable 动画时间-->
 * <attr name="foregroundDuration" format="integer"/>
 * <!--hover drawable 最小不透明度-->
 * <attr name="foregroundAlphaMin" format="integer"/>
 * <!--hover drawable 最大不透明度-->
 * <attr name="foregroundAlphaMax" format="integer"/>
 * <p>
 * <!--设置滑动时候的 EdgeEffect效果开关-->
 * <attr name="edgeEffectEnable" format="boolean"/>
 * <p>
 * <!--左边线的颜色，宽度，和边线padding-->
 * <attr name="borderLeftColor" format="color"/>
 * <attr name="borderLeftWidth" format="dimension"/>
 * <attr name="borderLeftMargin" format="dimension"/>
 * <attr name="borderLeftMarginStart" format="dimension"/>
 * <attr name="borderLeftMarginEnd" format="dimension"/>
 * <p>
 * <!--上边线的颜色，宽度，和边线padding-->
 * <attr name="borderTopColor" format="color"/>
 * <attr name="borderTopWidth" format="dimension"/>
 * <attr name="borderTopMargin" format="dimension"/>
 * <attr name="borderTopMarginStart" format="dimension"/>
 * <attr name="borderTopMarginEnd" format="dimension"/>
 * <p>
 * <!--右边线的颜色，宽度，和边线padding-->
 * <attr name="borderRightColor" format="color"/>
 * <attr name="borderRightWidth" format="dimension"/>
 * <attr name="borderRightMargin" format="dimension"/>
 * <attr name="borderRightMarginStart" format="dimension"/>
 * <attr name="borderRightMarginEnd" format="dimension"/>
 * <p>
 * <!--下边线的颜色，宽度，和边线padding-->
 * <attr name="borderBottomColor" format="color"/>
 * <attr name="borderBottomWidth" format="dimension"/>
 * <attr name="borderBottomMargin" format="dimension"/>
 * <attr name="borderBottomMarginStart" format="dimension"/>
 * <attr name="borderBottomMarginEnd" format="dimension"/>
 * <p>
 * <!--内容四边的间距，不同于padding -->
 * <attr name="contentMarginLeft" format="dimension"/>
 * <attr name="contentMarginTop" format="dimension"/>
 * <attr name="contentMarginRight" format="dimension"/>
 * <attr name="contentMarginBottom" format="dimension"/>
 * <!--水平方向和垂直方向Item 的间距-->
 * <attr name="contentMarginHorizontal" format="dimension"/>
 * <attr name="contentMarginVertical" format="dimension"/>
 * <p>
 * <!--水平分割线颜色-->
 * <attr name="dividerColorHorizontal" format="color"/>
 * <!--水平分割线宽-->
 * <attr name="dividerWidthHorizontal" format="dimension"/>
 * <!--水平分割线开始和结束padding-->
 * <attr name="dividerPaddingHorizontal" format="dimension"/>
 * <attr name="dividerPaddingHorizontalStart" format="dimension"/>
 * <attr name="dividerPaddingHorizontalEnd" format="dimension"/>
 * <p>
 * <!--垂直分割线颜色-->
 * <attr name="dividerColorVertical" format="color"/>
 * <!--垂直分割线宽-->
 * <attr name="dividerWidthVertical" format="dimension"/>
 * <!--垂直分割线开始 和结束padding-->
 * <attr name="dividerPaddingVertical" format="dimension"/>
 * <attr name="dividerPaddingVerticalStart" format="dimension"/>
 * <attr name="dividerPaddingVerticalEnd" format="dimension"/>
 * </declare-styleable>
 *
 * @author: rexy
 * @date: 2017-04-25 09:32
 */
public abstract class BaseViewGroup extends ViewGroup {
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    private static final int[] ATTRS_PROPERTIES = new int[]
            {android.R.attr.gravity, android.R.attr.maxWidth, android.R.attr.maxHeight, android.R.attr.orientation, android.R.attr.clipToPadding};
    private static final int[] ATTRS_PARAMS = new int[]
            {android.R.attr.layout_gravity, android.R.attr.maxWidth, android.R.attr.maxHeight};

    int mGravity;
    int mMaxWidth = -1;
    int mMaxHeight = -1;
    int mOrientation;
    boolean mClipToPadding;
    boolean mEdgeEffectEnable;

    boolean mIgnoreForegroundStateWhenTouchOut = false;
    FloatDrawable mForegroundDrawable = null;


    boolean mTouchScrollEnable = true;
    boolean mCanTouchScrollHorizontal = false;
    boolean mCanTouchScrollVertical = false;
    OnItemTouchListener mItemTouchListener;
    DrawerDecoration mDrawerDecoration;


    protected int mTouchSlop = 0;
    protected DividerMargin mDividerMargin = null;

    private int mVirtualCount = 0;
    private int mContentWidth = 0;
    private int mContentHeight = 0;
    private int mMeasureState = 0;
    private Rect mContentInset = new Rect();
    private Rect mVisibleContentBounds = new Rect();

    private boolean mAttachLayout = false;
    private boolean mItemTouchInvoked = false;

    private String mLogTag;
    private boolean mDevLog = true;
    private long mTimeMeasureStart, mTimeLayoutStart, mTimeDrawStart;
    long mLastMeasureCost, mLastLayoutCost, mLastDrawCost;


    public BaseViewGroup(Context context) {
        super(context);
        init(context, null);
    }

    public BaseViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BaseViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public BaseViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        TypedArray a = attrs == null ? null : context.obtainStyledAttributes(attrs, R.styleable.BaseViewGroup);
        mDividerMargin = DividerMargin.from(context, a);
        if (a != null) {
            mEdgeEffectEnable = a.getBoolean(R.styleable.BaseViewGroup_edgeEffectEnable, mEdgeEffectEnable);
            int floatColor = a.getColor(R.styleable.BaseViewGroup_foregroundColor, 0);
            if (floatColor != 0) {
                int floatRadius = a.getDimensionPixelSize(R.styleable.BaseViewGroup_foregroundRadius, 0);
                int floatDuration = a.getInt(R.styleable.BaseViewGroup_foregroundDuration, 120);
                int floatMinAlpha = a.getInt(R.styleable.BaseViewGroup_foregroundAlphaMin, 0);
                int floatMaxAlpha = a.getInt(R.styleable.BaseViewGroup_foregroundAlphaMax, 50);
                FloatDrawable floatDrawable = new FloatDrawable(floatColor, floatMinAlpha, floatMaxAlpha).duration(floatDuration).radius(floatRadius);
                setForegroundDrawable(floatDrawable);
                setClickable(true);
            }
            a.recycle();
        }
        a = attrs == null ? null : context.obtainStyledAttributes(attrs, ATTRS_PROPERTIES);
        if (a != null) {
            mGravity = a.getInt(0, mGravity);
            mMaxWidth = a.getDimensionPixelSize(1, mMaxWidth);
            mMaxHeight = a.getDimensionPixelSize(2, mMaxHeight);
            mOrientation = a.getInt(3, mOrientation - 1) + 1;
            mClipToPadding = a.getBoolean(4, true);
            a.recycle();
        }
    }

    //start:log
    protected boolean isLogAccess() {
        return mLogTag != null;
    }

    protected boolean isDevLogAccess() {
        return mLogTag != null && mDevLog;
    }

    public void setLogTag(String logTag, boolean devMode) {
        mLogTag = logTag;
        mDevLog = devMode;
    }

    protected void print(CharSequence category, CharSequence msg) {
        print(category, msg, false);
    }

    void printDev(CharSequence category, CharSequence msg) {
        print(category, msg, true);
    }

    private void print(CharSequence category, CharSequence msg, boolean dev) {
        String tag = mLogTag + (dev ? "@" : "#");
        if (category == null || msg == null) {
            msg = category == null ? msg : category;
        } else {
            tag = tag + category;
        }
        Log.d(tag, String.valueOf(msg));
    }
    //end:log

    protected void requestLayoutIfNeed() {
        if (!isLayoutRequested()) {
            requestLayout();
        }
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (clipToPadding != mClipToPadding) {
            super.setClipToPadding(clipToPadding);
            mClipToPadding = clipToPadding;
            if (mAttachLayout) {
                invalidate();
            }
        }
    }

    @Override
    public boolean getClipToPadding() {
        return mClipToPadding;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayoutIfNeed();
        }
    }

    public int getGravity() {
        return mGravity;
    }

    public void setMaxWidth(int maxWidth) {
        if (mMaxWidth != maxWidth) {
            mMaxWidth = maxWidth;
            requestLayoutIfNeed();
        }
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxHeight(int maxHeight) {
        if (mMaxHeight != maxHeight) {
            mMaxHeight = maxHeight;
            requestLayoutIfNeed();
        }
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        orientation = orientation & (HORIZONTAL | VERTICAL);
        if (mOrientation != orientation) {
            int oldOrientation = mOrientation;
            mOrientation = orientation;
            mAttachLayout = false;
            onOrientationChanged(orientation, oldOrientation);
            requestLayoutIfNeed();
        }
    }

    public void setTouchScrollEnable(boolean touchScrollEnable) {
        if (mTouchScrollEnable != touchScrollEnable) {
            mTouchScrollEnable = touchScrollEnable;
        }
    }

    public boolean isTouchScrollEnable(boolean contentConsidered) {
        return isTouchScrollVerticalEnable(contentConsidered) || isTouchScrollHorizontalEnable(contentConsidered);
    }

    public void setTouchScrollHorizontalEnable(boolean canTouchScrollHorizontal) {
        if (mCanTouchScrollHorizontal != canTouchScrollHorizontal) {
            mCanTouchScrollHorizontal = canTouchScrollHorizontal;
        }
    }

    public boolean isTouchScrollHorizontalEnable(boolean contentConsidered) {
        boolean scrollOk = mTouchScrollEnable && mCanTouchScrollHorizontal;
        if (contentConsidered && scrollOk) {
            return getHorizontalScrollRange() > 0;
        }
        return scrollOk;
    }

    public void setTouchScrollVerticalEnable(boolean canTouchScrollVertical) {
        if (mCanTouchScrollVertical != canTouchScrollVertical) {
            mCanTouchScrollVertical = canTouchScrollVertical;
        }
    }

    public boolean isTouchScrollVerticalEnable(boolean contentConsidered) {
        boolean scrollOk = mTouchScrollEnable && mCanTouchScrollVertical;
        if (contentConsidered && scrollOk) {
            return getVerticalScrollRange() > 0;
        }
        return scrollOk;
    }

    protected void setContentSize(int contentWidth, int contentHeight, int measureState) {
        mContentWidth = contentWidth;
        mContentHeight = contentHeight;
        mMeasureState |= measureState;
    }

    public int getContentWidth() {
        return mContentWidth;
    }

    public int getContentPureWidth() {
        return mContentWidth - (mContentInset.left + mContentInset.right);
    }

    public int getContentHeight() {
        return mContentHeight;
    }

    public int getContentPureHeight() {
        return mContentHeight - (mContentInset.top + mContentInset.bottom);
    }

    protected int getMeasureState() {
        return mMeasureState;
    }

    public Rect getVisibleContentBounds() {
        return mVisibleContentBounds;
    }

    public boolean isAttachLayoutFinished() {
        return mAttachLayout;
    }

    public int getPaddingLeftWithInset() {
        return getPaddingLeft() + mContentInset.left;
    }

    public int getPaddingRightWithInset() {
        return getPaddingRight() + mContentInset.right;
    }

    public int getPaddingTopWithInset() {
        return getPaddingTop() + mContentInset.top;
    }

    public int getPaddingBottomWithInset() {
        return getPaddingBottom() + mContentInset.bottom;
    }

    public int getWidthWithoutPadding() {
        return mVisibleContentBounds.width();
    }

    public int getWidthWithoutPaddingInset() {
        return mVisibleContentBounds.width() - mContentInset.left - mContentInset.right;
    }

    public int getHeightWithoutPadding() {
        return mVisibleContentBounds.height();
    }

    public int getHeightWithoutPaddingInset() {
        return mVisibleContentBounds.height() - mContentInset.top - mContentInset.bottom;
    }

    public void setOnItemTouchListener(OnItemTouchListener itemTouchListener) {
        this.mItemTouchListener = itemTouchListener;
    }

    public void setDrawerDecoration(DrawerDecoration drawerDecoration) {
        if (mDrawerDecoration != drawerDecoration) {
            mDrawerDecoration = drawerDecoration;
            requestLayoutIfNeed();
        }
    }

    public void setForegroundDrawable(FloatDrawable foregroundDrawable) {
        if (mForegroundDrawable != foregroundDrawable) {
            if (mForegroundDrawable != null) {
                mForegroundDrawable.setCallback(null);
                unscheduleDrawable(mForegroundDrawable);
            }
            if (foregroundDrawable == null) {
                mForegroundDrawable = null;
            } else {
                mForegroundDrawable = foregroundDrawable;
                foregroundDrawable.setCallback(this);
                foregroundDrawable.setVisible(getVisibility() == VISIBLE, false);
            }
        }
    }

    public void setIgnoreForegroundStateWhenTouchOut(boolean ignoreForegroundStateWhenTouchOut) {
        mIgnoreForegroundStateWhenTouchOut = ignoreForegroundStateWhenTouchOut;
    }

    public FloatDrawable setForegroundDrawable(int color, int minAlpha, int maxAlpha) {
        FloatDrawable drawable = new FloatDrawable(color, minAlpha, maxAlpha);
        setForegroundDrawable(drawable);
        return drawable;
    }

    public boolean verifyFlag(int value, int flag) {
        return flag == (value & flag);
    }

    public boolean isIgnoreForegroundStateWhenTouchOut() {
        return mIgnoreForegroundStateWhenTouchOut;
    }

    public FloatDrawable getForegroundDrawable() {
        return mForegroundDrawable;
    }

    @Override
    public void removeAllViewsInLayout() {
        super.removeAllViewsInLayout();
        mAttachLayout = false;
        mVirtualCount = 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachLayout = false;
        mVirtualCount = 0;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachLayout = false;
        mVirtualCount = 0;
    }

    @Override
    public BaseViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new BaseViewGroup.LayoutParams(getContext(), attrs);
    }

    @Override
    protected BaseViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new BaseViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected BaseViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof MarginLayoutParams) {
            return new BaseViewGroup.LayoutParams((MarginLayoutParams) p);
        }
        return new BaseViewGroup.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof BaseViewGroup.LayoutParams;
    }

    //start:measure&layout&draw
    private int getMeasureSize(int minSize, int maxSize, int contentSize, int padding) {
        int finalSize = Math.max(minSize, contentSize + padding);
        if (maxSize > minSize && maxSize > 0 && finalSize > maxSize) {
            finalSize = maxSize;
        }
        return finalSize;
    }

    private int getMeasureSizeWithoutPadding(int minSize, int maxSize, int measureSpec, int padding) {
        int finalSize = MeasureSpec.getSize(measureSpec);
        if (minSize > finalSize) {
            finalSize = minSize;
        }
        if (finalSize > maxSize && maxSize > minSize && maxSize > 0) {
            finalSize = maxSize;
        }
        return Math.max(finalSize - padding, 0);
    }

    @Override
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTimeMeasureStart = System.currentTimeMillis();
        mVirtualCount = 0;
        mContentWidth = mContentHeight = mMeasureState = 0;
        final int minWidth = getSuggestedMinimumWidth();
        final int minHeight = getSuggestedMinimumHeight();
        final int paddingHorizontal = getPaddingLeft() + getPaddingRight();
        final int paddingVertical = getPaddingTop() + getPaddingBottom();
        final int mostWidthNoPadding = getMeasureSizeWithoutPadding(minWidth, mMaxWidth, widthMeasureSpec, paddingHorizontal);
        final int mostHeightNoPadding = getMeasureSizeWithoutPadding(minHeight, mMaxHeight, heightMeasureSpec, paddingVertical);
        mContentInset.setEmpty();
        if (mDrawerDecoration != null) {
            mDrawerDecoration.getContentOffsets(mContentInset, this, mostWidthNoPadding, mostHeightNoPadding);
        }
        mDividerMargin.applyContentMargin(mContentInset);
        final int contentMarginH = mContentInset.left + mContentInset.right;
        final int contentMarginV = mContentInset.top + mContentInset.bottom;
        dispatchMeasure(
                MeasureSpec.makeMeasureSpec(Math.max(0, mostWidthNoPadding - contentMarginH), MeasureSpec.getMode(widthMeasureSpec)),
                MeasureSpec.makeMeasureSpec(Math.max(0, mostHeightNoPadding - contentMarginV), MeasureSpec.getMode(heightMeasureSpec))
        );
        int contentWidth = mContentWidth + contentMarginH, contentHeight = mContentHeight + contentMarginV, childState = mMeasureState;
        setContentSize(contentWidth, contentHeight, childState);
        int finalWidth = getMeasureSize(minWidth, mMaxWidth, contentWidth, paddingHorizontal);
        int finalHeight = getMeasureSize(minHeight, mMaxHeight, contentHeight, paddingVertical);
        setMeasuredDimension(resolveSizeAndState(finalWidth, widthMeasureSpec, childState),
                resolveSizeAndState(finalHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
        int measuredWidth = getMeasuredWidth(), measuredHeight = getMeasuredHeight();
        computeVisibleBounds(getScrollX(), getScrollY(), false, false);
        mVisibleContentBounds.offset(1, 1);
        doAfterMeasure(measuredWidth, measuredHeight, contentWidth, contentHeight);
        mLastMeasureCost = System.currentTimeMillis() - mTimeMeasureStart;
        if (isDevLogAccess()) {
            printDev("MLD", String.format("measure cost %d ms: [width=%d,height=%d],[contentW=%d,contentH=%d]", mLastMeasureCost, measuredWidth, measuredHeight, contentWidth, contentHeight));
        }
    }

    private void updateItemInset(View child, Rect outRect, int position) {
        if (mDrawerDecoration != null) {
            mDrawerDecoration.getItemOffsets(outRect, child, position, this);
        }
    }

    /**
     * tips:measure no need to take content margin into account.
     * in this function must call setContentSize(contentWidth,contentHeight);
     *
     * @param widthMeasureSpecContent  widthMeasureSpec with out padding and content margin
     * @param heightMeasureSpecContent heightMeasureSpec with out padding and content margin.
     */
    protected abstract void dispatchMeasure(int widthMeasureSpecContent, int heightMeasureSpecContent);

    /**
     * @param measuredWidth  self measure width
     * @param measuredHeight self measure height
     * @param contentWidth   real content width and content margin horizontal sum
     * @param contentHeight  real content height and content margin vertical sum
     */
    protected void doAfterMeasure(int measuredWidth, int measuredHeight, int contentWidth, int contentHeight) {
    }

    @Override
    protected final void onLayout(boolean changed, int l, int t, int r, int b) {
        mTimeLayoutStart = System.currentTimeMillis();
        boolean firstAttachLayout = false;
        if (!mAttachLayout) {
            firstAttachLayout = mAttachLayout = true;
        }
        dispatchLayout(getContentLeft() + mContentInset.left, getContentTop() + mContentInset.top);
        if (firstAttachLayout) {
            computeVisibleBounds(getScrollX(), getScrollY(), false, true);
        }
        doAfterLayout(firstAttachLayout);
        mLastLayoutCost = System.currentTimeMillis() - mTimeLayoutStart;
        if (isDevLogAccess()) {
            printDev("MLD", String.format("layout cost %d ms: firstAttachLayout=%s", mLastLayoutCost, firstAttachLayout));
        }
    }

    /**
     * tips:should take content margin into accout when layout child.
     *
     * @param baseLeft format content's left no need to take margin and padding of content into account.
     * @param baseTop  format content's top no need to take margin and padding of content into account.
     */
    protected abstract void dispatchLayout(int baseLeft, int baseTop);

    protected void doAfterLayout(boolean firstAttachLayout) {
    }

    @Override
    public final void dispatchDraw(Canvas canvas) {
        mTimeDrawStart = System.currentTimeMillis();
        doBeforeDraw(canvas, mContentInset);
        super.dispatchDraw(canvas);
        doAfterDraw(canvas, mContentInset);
        mDividerMargin.drawBorder(canvas, getWidth(), getHeight());
        if (mForegroundDrawable != null) {
            mForegroundDrawable.setBounds(0, 0, getWidth(), getHeight());
            mForegroundDrawable.draw(canvas);
        }
        mLastDrawCost = System.currentTimeMillis() - mTimeDrawStart;
        if (isDevLogAccess()) {
            printDev("MLD", String.format("draw cost %d ms", mLastDrawCost));
        }
    }

    protected void doBeforeDraw(Canvas canvas, Rect inset) {
    }

    protected void doAfterDraw(Canvas canvas, Rect inset) {
    }
    //end:measure&layout&draw

    //start: touch gesture
    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (mItemTouchListener != null) {
            mItemTouchListener.onRequestDisallowInterceptTouchEvent(disallowIntercept);
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    protected boolean dispatchOnItemTouchIntercept(MotionEvent e) {
        final int action = e.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_DOWN) {
            mItemTouchInvoked = false;
        }
        if (mItemTouchListener != null) {
            if (mItemTouchListener.onInterceptTouchEvent(this, e) && action != MotionEvent.ACTION_CANCEL) {
                mItemTouchInvoked = true;
                return true;
            }
        }
        return false;
    }

    protected boolean dispatchOnItemTouch(MotionEvent e) {
        final int action = e.getAction();
        if (mItemTouchInvoked) {
            if (action == MotionEvent.ACTION_DOWN) {
                mItemTouchInvoked = false;
            } else {
                mItemTouchListener.onTouchEvent(this, e);
                if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    // Clean up for the next gesture.
                    mItemTouchInvoked = false;
                }
                return true;
            }
        }
        if (action != MotionEvent.ACTION_DOWN && mItemTouchListener != null) {
            if (mItemTouchListener.onInterceptTouchEvent(this, e)) {
                mItemTouchInvoked = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (dispatchOnItemTouchIntercept(e)) {
            return true;
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (dispatchOnItemTouch(e)) {
            dispatchDrawableTouch(null);
            return true;
        }
        dispatchDrawableTouch(e);
        return super.onTouchEvent(e);
    }

    protected void dispatchDrawableTouch(MotionEvent e) {
        if (mForegroundDrawable != null && isClickable()) {
            if (e == null) {
                mForegroundDrawable.start(false);
                return;
            }
            int action = e.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                mForegroundDrawable.start(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mForegroundDrawable.start(false);
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!mIgnoreForegroundStateWhenTouchOut && !pointInView(e.getX(), e.getY(), mTouchSlop)) {
                    mForegroundDrawable.start(false);
                }
            }
        }
    }

    protected void onOrientationChanged(int orientation, int oldOrientation) {
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        computeVisibleBounds(l, t, true, true);
    }

    protected void onScrollChanged(int scrollX, int scrollY, Rect visibleBounds, boolean fromScrollChanged) {

    }

    protected void computeVisibleBounds(int scrollX, int scrollY, boolean scrollChanged, boolean apply) {
        int beforeHash = mVisibleContentBounds.hashCode(), width = apply ? getWidth() : 0, height = apply ? getHeight() : 0;
        if (width <= 0) width = getMeasuredWidth();
        if (height <= 0) height = getMeasuredHeight();
        mVisibleContentBounds.left = getPaddingLeft() + scrollX;
        mVisibleContentBounds.top = getPaddingTop() + scrollY;
        mVisibleContentBounds.right = mVisibleContentBounds.left + width - getPaddingLeft() - getPaddingRight();
        mVisibleContentBounds.bottom = mVisibleContentBounds.top + height - getPaddingTop() - getPaddingBottom();
        if (apply && beforeHash != mVisibleContentBounds.hashCode()) {
            if (isDevLogAccess()) {
                StringBuilder sb = new StringBuilder(32);
                sb.append("scrollX=").append(scrollX);
                sb.append(",scrollY=").append(scrollY).append(",visibleBounds=").append(mVisibleContentBounds);
                sb.append(",scrollChanged=").append(scrollChanged);
                printDev("scroll", sb);
            }
            onScrollChanged(scrollX, scrollY, mVisibleContentBounds, scrollChanged);
        }
    }

    private boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((getRight() - getLeft()) + slop) &&
                localY < ((getBottom() - getTop()) + slop);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        boolean result = super.verifyDrawable(who);
        if (!result && mForegroundDrawable == who) {
            result = true;
        }
        return result;
    }
    //end: touch gesture

    // start: tool function
    protected final View getVirtualChildAt(int virtualIndex, boolean withoutGone) {
        int virtualCount = 0;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (skipVirtualChild(child, withoutGone)) continue;
            if (virtualCount == virtualIndex) {
                return child;
            }
            virtualCount++;
        }
        return null;
    }

    protected final int getVirtualChildCount(boolean withoutGone) {
        int virtualCount = 0;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (skipVirtualChild(child, withoutGone)) continue;
            virtualCount++;
        }
        return virtualCount;
    }

    public final int getItemViewCount() {
        int itemCount = mVirtualCount;
        if (itemCount == 0) {
            itemCount = mVirtualCount = getVirtualChildCount(true);
        }
        return itemCount;
    }

    public final View getItemView(int itemIndex) {
        View result = null;
        int itemCount = getItemViewCount();
        if (itemIndex >= 0 && itemIndex < itemCount) {
            result = getVirtualChildAt(itemIndex, true);
        }
        return result;
    }

    public final int indexOfItemView(View view) {
        if (view != null) {
            int virtualIndex = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (skipVirtualChild(child, true)) continue;
                if (view == child) {
                    return virtualIndex;
                }
                virtualIndex++;
            }
        }
        return -1;
    }

    protected boolean skipChild(View child) {
        return child == null || child.getVisibility() == View.GONE;
    }

    protected boolean skipVirtualChild(View child, boolean withoutGone) {
        return child == null || (withoutGone && child.getVisibility() == View.GONE);
    }

    protected int getContentLeft() {
        int paddingLeft = getPaddingLeft();
        int result = getContentStartH(paddingLeft, getWidth() - getPaddingRight(), getContentWidth(), mGravity);
        if (result < paddingLeft && isTouchScrollHorizontalEnable(false)) {
            result = paddingLeft;
        }
        return result;
    }

    protected int getContentTop() {
        int paddingTop = getPaddingTop();
        int result = getContentStartH(paddingTop, getHeight() - getPaddingBottom(), getContentHeight(), mGravity);
        if (result < paddingTop && isTouchScrollVerticalEnable(false)) {
            result = paddingTop;
        }
        return result;
    }

    protected int getContentStartH(int containerLeft, int containerRight, int contentWillSize, int gravity) {
        return getContentStartH(containerLeft, containerRight, contentWillSize, 0, 0, gravity);
    }

    protected int getContentStartV(int containerTop, int containerBottom, int contentWillSize, int gravity) {
        return getContentStartV(containerTop, containerBottom, contentWillSize, 0, 0, gravity);
    }

    protected int getContentStartH(int containerLeft, int containerRight, int contentWillSize, int contentMarginLeft, int contentMarginRight, int gravity) {
        if (gravity != -1 || gravity != 0) {
            int start;
            final int mask = Gravity.HORIZONTAL_GRAVITY_MASK;
            final int maskCenter = Gravity.CENTER_HORIZONTAL;
            final int maskEnd = Gravity.RIGHT;
            final int okGravity = gravity & mask;
            if (maskCenter == okGravity) {//center
                start = containerLeft + (containerRight - containerLeft - (contentWillSize + contentMarginRight - contentMarginLeft)) / 2;
            } else if (maskEnd == okGravity) {//end
                start = containerRight - contentWillSize - contentMarginRight;
            } else {//start
                start = containerLeft + contentMarginLeft;
            }
            return start;
        }
        return containerLeft + contentMarginLeft;
    }

    protected int getContentStartV(int containerTop, int containerBottom, int contentWillSize, int contentMarginTop, int contentMarginBottom, int gravity) {
        if (gravity != -1 || gravity != 0) {
            int start;
            final int mask = Gravity.VERTICAL_GRAVITY_MASK;
            final int maskCenter = Gravity.CENTER_VERTICAL;
            final int maskEnd = Gravity.BOTTOM;
            final int okGravity = gravity & mask;
            if (maskCenter == okGravity) {//center
                start = containerTop + (containerBottom - containerTop - (contentWillSize + contentMarginBottom - contentMarginTop)) / 2;
            } else if (maskEnd == okGravity) {//end
                start = containerBottom - contentWillSize - contentMarginBottom;
            } else {//start
                start = containerTop + contentMarginTop;
            }
            return start;
        }
        return containerTop + contentMarginTop;
    }

    protected int offsetX(View child, boolean centreInVisibleBounds, boolean marginInclude) {
        int current;
        MarginLayoutParams marginLp = marginInclude ? (MarginLayoutParams) child.getLayoutParams() : null;
        if (centreInVisibleBounds) {
            current = (child.getLeft() + child.getRight()) >> 1;
            if (marginLp != null) {
                current = current + (marginLp.rightMargin - marginLp.leftMargin) / 2;
            }
            return current - mVisibleContentBounds.centerX() + mVisibleContentBounds.left - getPaddingLeft();
        } else {
            current = child.getLeft();
            if (marginLp != null) {
                current = current - marginLp.leftMargin;
            }
            return current - getPaddingLeft();
        }
    }

    protected int offsetY(View child, boolean centreInVisibleBounds, boolean marginInclude) {
        int current;
        MarginLayoutParams marginLp = marginInclude ? (MarginLayoutParams) child.getLayoutParams() : null;
        if (centreInVisibleBounds) {
            current = (child.getTop() + child.getBottom()) >> 1;
            if (marginLp != null) {
                current = current + (marginLp.bottomMargin - marginLp.topMargin) / 2;
            }
            return current - mVisibleContentBounds.centerY() + mVisibleContentBounds.top - getPaddingTop();
        } else {
            current = child.getTop();
            if (marginLp != null) {
                current = current - marginLp.topMargin;
            }
            return current - getPaddingTop();
        }
    }
    // end: tool function


    //start:compute scroll information.
    protected int getVerticalScrollRange() {
        int scrollRange = 0, contentSize = getContentHeight();
        if (contentSize > 0) {
            scrollRange = contentSize - mVisibleContentBounds.height();
            if (scrollRange < 0) {
                scrollRange = 0;
            }
        }
        return scrollRange;
    }

    @Override
    public int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int paddingTop = getPaddingTop();
        final int contentHeight = mVisibleContentBounds.height();
        if (count == 0) {
            return contentHeight;
        }
        int scrollRange = paddingTop + getContentHeight();
        final int scrollY = getScrollY();
        final int overScrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overScrollBottom) {
            scrollRange += scrollY - overScrollBottom;
        }
        return scrollRange;
    }

    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    public int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int paddingLeft = getPaddingLeft();
        final int contentWidth = mVisibleContentBounds.width();
        if (count == 0) {
            return contentWidth;
        }
        int scrollRange = paddingLeft + getContentWidth();
        final int scrollX = getScrollX();
        final int overScrollRight = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overScrollRight) {
            scrollRange += scrollX - overScrollRight;
        }
        return scrollRange;
    }

    protected int getHorizontalScrollRange() {
        int scrollRange = 0, contentSize = getContentWidth();
        if (contentSize > 0) {
            scrollRange = contentSize - mVisibleContentBounds.width();
            if (scrollRange < 0) {
                scrollRange = 0;
            }
        }
        return scrollRange;
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    //start:DividerMargin
    public int getBorderLeftColor() {
        return mDividerMargin.getBorderLeftColor();
    }

    public int getBorderLeftWidth() {
        return mDividerMargin.getBorderLeftWidth();
    }

    public int getBorderLeftMarginStart() {
        return mDividerMargin.getBorderLeftMarginStart();
    }

    public int getBorderLeftMarginEnd() {
        return mDividerMargin.getBorderLeftMarginEnd();
    }

    public int getBorderTopColor() {
        return mDividerMargin.getBorderTopColor();
    }

    public int getBorderTopWidth() {
        return mDividerMargin.getBorderTopWidth();
    }

    public int getBorderTopMarginStart() {
        return mDividerMargin.getBorderTopMarginStart();
    }

    public int getBorderTopMarginEnd() {
        return mDividerMargin.getBorderTopMarginEnd();
    }

    public int getBorderRightColor() {
        return mDividerMargin.getBorderRightColor();
    }

    public int getBorderRightWidth() {
        return mDividerMargin.getBorderRightWidth();
    }

    public int getBorderRightMarginStart() {
        return mDividerMargin.getBorderRightMarginStart();
    }

    public int getBorderRightMarginEnd() {
        return mDividerMargin.getBorderRightMarginEnd();
    }

    public int getBorderBottomColor() {
        return mDividerMargin.getBorderBottomColor();
    }

    public int getBorderBottomWidth() {
        return mDividerMargin.getBorderBottomWidth();
    }

    public int getBorderBottomMarginStart() {
        return mDividerMargin.getBorderBottomMarginStart();
    }

    public int getBorderBottomMarginEnd() {
        return mDividerMargin.getBorderBottomMarginEnd();
    }

    public int getContentMarginMiddleHorizontal() {
        return mDividerMargin.getContentMarginHorizontal();
    }

    public int getContentMarginMiddleVertical() {
        return mDividerMargin.getContentMarginVertical();
    }

    public int getContentMarginLeft() {
        return mDividerMargin.getContentMarginLeft();
    }

    public int getContentMarginTop() {
        return mDividerMargin.getContentMarginTop();
    }

    public int getContentMarginRight() {
        return mDividerMargin.getContentMarginRight();
    }

    public int getContentMarginBottom() {
        return mDividerMargin.getContentMarginBottom();
    }

    public int getDividerWidthHorizontal() {
        return mDividerMargin.getDividerWidthHorizontal();
    }

    public int getDividerWidthVertical() {
        return mDividerMargin.getDividerWidthVertical();
    }

    public int getDividerColorHorizontal() {
        return mDividerMargin.getDividerColorHorizontal();
    }

    public int getDividerColorVertical() {
        return mDividerMargin.getDividerColorVertical();
    }

    public int getDividerPaddingStartHorizontal() {
        return mDividerMargin.getDividerPaddingHorizontalStart();
    }

    public int getDividerPaddingStartVertical() {
        return mDividerMargin.getDividerPaddingVerticalStart();
    }

    public int getDividerPaddingEndHorizontal() {
        return mDividerMargin.getDividerPaddingHorizontalEnd();
    }

    public int getDividerPaddingEndVertical() {
        return mDividerMargin.getDividerPaddingVerticalEnd();
    }

    public void setBorderLeftColor(int color) {
        mDividerMargin.setBorderLeftColor(color);
        invalidate();
    }

    public void setBorderLeftWidth(int width) {
        mDividerMargin.setBorderLeftWidth(width);
        invalidate();
    }

    public void setBorderLeftMarginStart(int marginStart) {
        mDividerMargin.setBorderLeftMarginStart(marginStart);
        invalidate();
    }

    public void setBorderLeftMarginEnd(int marginEnd) {
        mDividerMargin.setBorderLeftMarginEnd(marginEnd);
        invalidate();
    }

    public void setBorderTopColor(int color) {
        mDividerMargin.setBorderTopColor(color);
        invalidate();
    }

    public void setBorderTopWidth(int width) {
        mDividerMargin.setBorderTopWidth(width);
        invalidate();
    }

    public void setBorderTopMarginStart(int marginStart) {
        mDividerMargin.setBorderTopMarginStart(marginStart);
        invalidate();
    }

    public void setBorderTopMarginEnd(int marginEnd) {
        mDividerMargin.setBorderTopMarginEnd(marginEnd);
        invalidate();
    }

    public void setBorderRightColor(int color) {
        mDividerMargin.setBorderRightColor(color);
        invalidate();
    }

    public void setBorderRightWidth(int width) {
        mDividerMargin.setBorderRightWidth(width);
        invalidate();
    }

    public void setBorderRightMarginStart(int marginStart) {
        mDividerMargin.setBorderRightMarginStart(marginStart);
        invalidate();
    }

    public void setBorderRightMarginEnd(int marginEnd) {
        mDividerMargin.setBorderRightMarginEnd(marginEnd);
        invalidate();
    }

    public void setBorderBottomColor(int color) {
        mDividerMargin.setBorderBottomColor(color);
        invalidate();
    }

    public void setBorderBottomWidth(int width) {
        mDividerMargin.setBorderBottomWidth(width);
        invalidate();
    }

    public void setBorderBottomMarginStart(int marginStart) {
        mDividerMargin.setBorderBottomMarginStart(marginStart);
        invalidate();
    }

    public void setBorderBottomMarginEnd(int marginEnd) {
        mDividerMargin.setBorderBottomMarginEnd(marginEnd);
        invalidate();
    }

    public void setContentMarginMiddleHorizontal(int contentMarginMiddleHorizontal) {
        if (contentMarginMiddleHorizontal != mDividerMargin.getContentMarginHorizontal()) {
            mDividerMargin.setContentMarginHorizontal(contentMarginMiddleHorizontal);
            requestLayoutIfNeed();
        }
    }

    public void setContentMarginMiddleVertical(int contentMarginMiddleVertical) {
        if (contentMarginMiddleVertical != mDividerMargin.getContentMarginVertical()) {
            mDividerMargin.setContentMarginVertical(contentMarginMiddleVertical);
            requestLayoutIfNeed();
        }
    }

    public void setContentMarginLeft(int contentMarginLeft) {
        if (contentMarginLeft != mDividerMargin.getContentMarginLeft()) {
            mDividerMargin.setContentMarginLeft(contentMarginLeft);
            requestLayoutIfNeed();
        }
    }

    public void setContentMarginTop(int contentMarginTop) {
        if (contentMarginTop != mDividerMargin.getContentMarginTop()) {
            mDividerMargin.setContentMarginTop(contentMarginTop);
            requestLayoutIfNeed();
        }
    }

    public void setContentMarginRight(int contentMarginRight) {
        if (contentMarginRight != mDividerMargin.getContentMarginRight()) {
            mDividerMargin.setContentMarginRight(contentMarginRight);
            requestLayoutIfNeed();
        }
    }

    public void setContentMarginBottom(int contentMarginBottom) {
        if (contentMarginBottom != mDividerMargin.getContentMarginBottom()) {
            mDividerMargin.setContentMarginBottom(contentMarginBottom);
            requestLayoutIfNeed();
        }
    }

    public void setDividerWidthHorizontal(int dividerWidthHorizontal) {
        mDividerMargin.setDividerWidthHorizontal(dividerWidthHorizontal);
        invalidate();
    }

    public void setDividerWidthVertical(int dividerWidthVertical) {
        mDividerMargin.setDividerWidthVertical(dividerWidthVertical);
        invalidate();
    }

    public void setDividerColorHorizontal(int dividerColorHorizontal) {
        mDividerMargin.setDividerColorHorizontal(dividerColorHorizontal);
        invalidate();
    }

    public void setDividerColorVertical(int dividerColorVertical) {
        mDividerMargin.setDividerColorVertical(dividerColorVertical);
        invalidate();
    }

    public void setDividerPaddingStartHorizontal(int dividerPaddingStartHorizontal) {
        mDividerMargin.setDividerPaddingHorizontalStart(dividerPaddingStartHorizontal);
        invalidate();
    }

    public void setDividerPaddingStartVertical(int dividerPaddingStartVertical) {
        mDividerMargin.setDividerPaddingVerticalStart(dividerPaddingStartVertical);
        invalidate();
    }

    public void setDividerPaddingEndHorizontal(int dividerPaddingEndHorizontal) {
        mDividerMargin.setDividerPaddingHorizontalEnd(dividerPaddingEndHorizontal);
        invalidate();
    }

    public void setDividerPaddingEndVertical(int dividerPaddingEndVertical) {
        mDividerMargin.setDividerPaddingVerticalEnd(dividerPaddingEndVertical);
        invalidate();
    }
    //end:DividerMargin

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = -1;
        public int maxWidth = -1;
        public int maxHeight = -1;
        private int mPosition = -1;

        private Rect mInsets = new Rect();

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, ATTRS_PARAMS);
            gravity = a.getInt(0, gravity);
            maxWidth = a.getDimensionPixelSize(1, maxWidth);
            maxHeight = a.getDimensionPixelSize(2, maxHeight);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            if (source instanceof BaseViewGroup.LayoutParams) {
                BaseViewGroup.LayoutParams lp = (BaseViewGroup.LayoutParams) source;
                gravity = lp.gravity;
                maxWidth = lp.maxWidth;
                maxHeight = lp.maxHeight;
            } else {
                if (source instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) source;
                    gravity = lp.gravity;
                }
                if (source instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) source;
                    gravity = lp.gravity;
                }
            }
        }

        public int position() {
            return mPosition;
        }

        public int width(View view) {
            return view.getMeasuredWidth() + leftMargin + rightMargin + mInsets.left + mInsets.right;
        }

        public int height(View view) {
            return view.getMeasuredHeight() + topMargin + bottomMargin + mInsets.top + mInsets.bottom;
        }

        public int leftMargin() {
            return leftMargin + mInsets.left;
        }

        public int topMargin() {
            return topMargin + mInsets.top;
        }

        public int rightMargin() {
            return rightMargin + mInsets.right;
        }

        public int bottomMargin() {
            return bottomMargin + mInsets.bottom;
        }


        public void measure(View child, int itemPosition, int childWidthMeasureSpec, int childHeightMeasureSpec) {
            mPosition = itemPosition;
            mInsets.setEmpty();
            if (child.getParent() instanceof BaseViewGroup) {
                ((BaseViewGroup) child.getParent()).updateItemInset(child, mInsets, mPosition);
            }
            int marginInsetH = mInsets.left + mInsets.right + leftMargin + rightMargin;
            int marginInsetV = mInsets.top + mInsets.bottom + topMargin + bottomMargin;
            childWidthMeasureSpec = limitMeasureSpec(childWidthMeasureSpec, maxWidth, marginInsetH, width == -1);
            childHeightMeasureSpec = limitMeasureSpec(childHeightMeasureSpec, maxHeight, marginInsetV, height == -1);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }

        public void measure(View view, int itemPosition, int parentWidthMeasureSpec, int parentHeightMeasureSpec, int widthUsed, int heightUsed) {
            measure(view, itemPosition
                    , BaseViewGroup.getChildMeasureSpec(parentWidthMeasureSpec, widthUsed, width)
                    , BaseViewGroup.getChildMeasureSpec(parentHeightMeasureSpec, heightUsed, height));
        }

        private int limitMeasureSpec(int measureSpec, int maxSize, int used, boolean mostToExactly) {
            int size = MeasureSpec.getSize(measureSpec) - used;
            int mode = MeasureSpec.getMode(measureSpec);
            if (size < 0) size = 0;
            if (maxSize > 0 && size > maxSize) size = maxSize;
            if (mostToExactly) {
                if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.UNSPECIFIED) {
                    mode = MeasureSpec.EXACTLY;
                }
            }
            if (maxSize > 0 && mode == MeasureSpec.UNSPECIFIED) {
                mode = MeasureSpec.AT_MOST;
            }
            return MeasureSpec.makeMeasureSpec(size < 0 ? 0 : size, mode);
        }
    }

    public interface OnItemTouchListener {

        boolean onInterceptTouchEvent(BaseViewGroup parent, MotionEvent e);

        void onTouchEvent(BaseViewGroup parent, MotionEvent e);

        void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept);
    }

    public static abstract class DrawerDecoration {
        public void onDraw(Canvas c, BaseViewGroup parent) {
        }

        public void onDrawOver(Canvas c, BaseViewGroup parent) {
        }

        public void getItemOffsets(Rect outRect, View child, int itemPosition, BaseViewGroup parent) {
            outRect.set(0, 0, 0, 0);
        }

        public void getContentOffsets(Rect outRect, BaseViewGroup parent, int widthNoPadding, int heightNoPadding) {
            outRect.set(0, 0, 0, 0);
        }
    }
}
