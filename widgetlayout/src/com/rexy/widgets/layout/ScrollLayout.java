package com.rexy.widgets.layout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

/**
 * 支持容器内容自身的gravity,maxWidth,maxHeight.
 * 支持直接子 View 的 layout_gravity,maxWidth,maxHeight 等。
 * 支持水平和垂直视图的滑动计算 Api 和滑动事件。
 * 随时可监听当前视图可见区域的变法。
 * onMeasure 和 onLayout 内部做了一定的通用处理，不可重载，可打印他们执行的结果和耗费时间。
 * <p>
 * <p>
 * 实现子类需要重写dispatchMeasure和dispatchLayout 两个方法。
 * 其中dispatchMeasure来实现child 的测量，最终需要调用setContentSize 方法。
 *
 * @author: rexy
 * @date: 2017-04-25 09:32
 */
public class ScrollLayout extends BaseViewGroup implements ScrollingView, NestedScrollingChild {
    private static final int INVALID_POINTER = -1;
    private static final int MAX_SCROLL_DURATION = 2000;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    private static final Interpolator DefaultInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    OnScrollChangeListener mScrollListener;

    private int mScrollState = SCROLL_STATE_IDLE;
    protected int mTouchSlop;
    protected int mMinFlingVelocity;
    protected int mMaxFlingVelocity;
    private VelocityTracker mVelocityTracker;
    private FlingScroller mFlingScroller = new FlingScroller();
    private int mLastTouchX;
    private int mLastTouchY;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mScrollPointerId = INVALID_POINTER;
    private Interpolator mInterpolator;
    private Rect mScrollInfo = new Rect();

    private final boolean[] mCanTouchScroll = new boolean[2];
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];

    private NestedScrollingChildHelper mScrollingChildHelper;

    private EdgeEffectCompat mLeftGlow, mTopGlow, mRightGlow, mBottomGlow;

    public ScrollLayout(Context context) {
        super(context);
        init(context, null);
    }

    public ScrollLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public ScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        setTouchScrollEnable(mTouchScrollEnable);
    }

    @Override
    public void setTouchScrollEnable(boolean touchScrollEnable) {
        super.setTouchScrollEnable(touchScrollEnable);
        if (isOrientationHorizontal()) {
            setTouchScrollHorizontalEnable(touchScrollEnable);
        }
        if (isOrientationVertical()) {
            setTouchScrollVerticalEnable(touchScrollEnable);
        }
    }

    public boolean isEdgeEffectEnable() {
        return mEdgeEffectEnable;
    }

    public void setEdgeEffectEnable(boolean enable) {
        if (mEdgeEffectEnable != enable) {
            mEdgeEffectEnable = enable;
            if (!enable) {
                invalidateGlows();
            }
            invalidate();
        }
    }

    @Override
    protected void onOrientationChanged(int orientation, int oldOrientation) {
        setTouchScrollEnable(isTouchScrollEnable(false));
        scrollTo(0, 0);
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (clipToPadding != getClipToPadding()) {
            invalidateGlows();
        }
        super.setClipToPadding(clipToPadding);
    }

    protected void setScrollState(int newState) {
        if (mScrollState != newState) {
            int preState = mScrollState;
            mScrollState = newState;
            if (isDevLogAccess()) {
                printDev("state", String.format("from %d to %d", preState, newState));
            }
            onScrollStateChanged(newState, preState);
            if (mScrollListener != null) {
                mScrollListener.onScrollStateChanged(mScrollState, preState);
            }
        }
    }

    public int getScrollState() {
        return mScrollState;
    }

    public boolean isOrientationHorizontal() {
        return orientationMatch(HORIZONTAL, false);
    }

    public boolean isOrientationVertical() {
        return orientationMatch(VERTICAL, true);
    }

    public boolean orientationMatch(int orientation, boolean defaultIfUnset) {
        int value = getOrientation();
        if (value == 0) {
            return defaultIfUnset;
        }
        return verifyFlag(value, orientation);
    }

    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        mScrollListener = l;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        invalidateGlows();
        mScrollInfo.setEmpty();
        mScrollingChildHelper = null;
    }

    @Override
    public void removeAllViewsInLayout() {
        super.removeAllViewsInLayout();
        invalidateGlows();
        mScrollInfo.setEmpty();
        mScrollingChildHelper = null;
    }

    //start:measure&layout&draw
    @Override
    protected void dispatchMeasure(int widthMeasureSpecContent, int heightMeasureSpecContent) {
        final int childCount = getChildCount();
        int contentWidth = 0, contentHeight = 0, childState = 0;
        int itemPosition = 0, itemMargin;
        if (isOrientationHorizontal()) {
            itemMargin = mBorderDivider.getContentMarginHorizontal();
            widthMeasureSpecContent = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpecContent), MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (skipChild(child)) continue;
                if (itemPosition != 0) contentWidth += itemMargin;
                ScrollLayout.LayoutParams params = (ScrollLayout.LayoutParams) child.getLayoutParams();
                params.measure(child, itemPosition++, widthMeasureSpecContent, heightMeasureSpecContent, 0, 0);
                contentWidth += params.width(child);
                int itemHeight = params.height(child);
                if (contentHeight < itemHeight) {
                    contentHeight = itemHeight;
                }
                childState |= child.getMeasuredState();
            }
        } else {
            itemMargin = mBorderDivider.getContentMarginVertical();
            heightMeasureSpecContent = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpecContent), MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (skipChild(child)) continue;
                if (itemPosition != 0) contentHeight += itemMargin;
                ScrollLayout.LayoutParams params = (ScrollLayout.LayoutParams) child.getLayoutParams();
                params.measure(child, itemPosition++, widthMeasureSpecContent, heightMeasureSpecContent, 0, 0);
                contentHeight += params.height(child);
                int itemWidth = params.width(child);
                if (contentWidth < itemWidth) {
                    contentWidth = itemWidth;
                }
                childState |= child.getMeasuredState();
            }
        }
        setContentSize(contentWidth, contentHeight, childState);
    }

    @Override
    protected void dispatchLayout(int contentLeft, int contentTop) {
        final int count = getChildCount();
        int childLeft = contentLeft, childTop = contentTop, childRight, childBottom, itemMargin;
        if (isOrientationHorizontal()) {
            itemMargin = mBorderDivider.getContentMarginHorizontal();
            final int baseTop = contentTop, baseBottom = contentTop + getContentPureHeight();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (skipChild(child)) continue;
                ScrollLayout.LayoutParams params = (ScrollLayout.LayoutParams) child.getLayoutParams();
                childTop = getContentStartH(baseTop, baseBottom, child.getMeasuredHeight(), params.topMargin(), params.bottomMargin(), params.gravity);
                childBottom = childTop + child.getMeasuredHeight();
                childLeft += params.leftMargin();
                childRight = childLeft + child.getMeasuredWidth();
                child.layout(childLeft, childTop, childRight, childBottom);
                childLeft = childRight + params.rightMargin + itemMargin;
            }
        } else {
            itemMargin = mBorderDivider.getContentMarginVertical();
            final int baseLeft = contentLeft, baseRight = contentLeft + getContentPureWidth();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (skipChild(child)) continue;
                ScrollLayout.LayoutParams params = (ScrollLayout.LayoutParams) child.getLayoutParams();
                childTop += params.topMargin();
                childBottom = childTop + child.getMeasuredHeight();
                childLeft = getContentStartH(baseLeft, baseRight, child.getMeasuredWidth(), params.leftMargin(), params.rightMargin(), params.gravity);
                childRight = childLeft + child.getMeasuredWidth();
                child.layout(childLeft, childTop, childRight, childBottom);
                childTop = childBottom + params.bottomMargin() + itemMargin;
            }
        }
    }

    protected void doAfterLayout(boolean firstAttachLayout) {
        if (0x80000000 == (0x80000000 & mScrollInfo.left)) {
            scrollToItem(mScrollInfo, true);
        }
    }

    @Override
    protected void doAfterDraw(Canvas c, Rect inset) {
        if (mEdgeEffectEnable) {
            // TODO If padding is not 0 and clipChildrenToPadding is false, to draw glows properly, we
            // need find children closest to edges. Not sure if it is worth the effort.
            boolean clipToPadding = getClipToPadding();
            boolean needsInvalidate = false;
            if (mLeftGlow != null && !mLeftGlow.isFinished()) {
                final int restore = c.save();
                final int padding = clipToPadding ? getPaddingBottom() : 0;
                c.rotate(270);
                c.translate(-getHeight() + padding, 0);
                needsInvalidate = mLeftGlow != null && mLeftGlow.draw(c);
                c.restoreToCount(restore);
            }
            if (mTopGlow != null && !mTopGlow.isFinished()) {
                final int restore = c.save();
                if (clipToPadding) {
                    c.translate(getPaddingLeft(), getPaddingTop());
                }
                needsInvalidate |= mTopGlow != null && mTopGlow.draw(c);
                c.restoreToCount(restore);
            }
            if (mRightGlow != null && !mRightGlow.isFinished()) {
                final int restore = c.save();
                final int width = getWidth();
                final int padding = clipToPadding ? getPaddingTop() : 0;
                c.rotate(90);
                c.translate(-padding, -width - getScrollX());
                needsInvalidate |= mRightGlow != null && mRightGlow.draw(c);
                c.restoreToCount(restore);
            }
            if (mBottomGlow != null && !mBottomGlow.isFinished()) {
                final int restore = c.save();
                c.rotate(180);
                if (clipToPadding) {
                    c.translate(-getWidth() + getPaddingRight(), -getHeight() + getPaddingBottom() - getScrollY());
                } else {
                    c.translate(-getWidth(), -getHeight());
                }
                needsInvalidate |= mBottomGlow != null && mBottomGlow.draw(c);
                c.restoreToCount(restore);
            }
            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }
    //end:measure&layout&draw

    //start: touch gesture

    protected boolean ignoreSelfTouch(boolean fromIntercept, MotionEvent e) {
        return !isTouchScrollEnable(false);
    }

    protected boolean ignoreSelfFling(int velocityX, int velocityY) {
        return (velocityX == 0 || !isTouchScrollHorizontalEnable(false)) && (velocityY == 0 || !isTouchScrollVerticalEnable(false));
    }

    protected boolean willDragging(int lastMoved, boolean canScrollContent, boolean horizontal) {
        return canScrollContent && Math.abs(lastMoved) > mTouchSlop;
    }

    protected int willScrollHorizontal(int dx) {
        int scroll = getScrollX();
        int willScroll = Math.min(Math.max(scroll + dx, 0), getHorizontalScrollRange());
        return willScroll - scroll;
    }

    protected int willScrollVertical(int dy) {
        int scroll = getScrollY();
        int willScroll = Math.min(Math.max(scroll + dy, 0), getVerticalScrollRange());
        return willScroll - scroll;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (dispatchOnItemTouchIntercept(e)) {
            cancelTouch(true);
            return true;
        }
        if (ignoreSelfTouch(true, e)) {
            cancelTouch(true);
            return super.onInterceptTouchEvent(e);
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);
        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCanTouchScroll[0] = isTouchScrollHorizontalEnable(true);
                mCanTouchScroll[1] = isTouchScrollVerticalEnable(true);
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
                if (mScrollState == SCROLL_STATE_SETTLING) {
                    mFlingScroller.stop();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                }
                mNestedOffsets[0] = mNestedOffsets[1] = 0;
                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (mCanTouchScroll[0]) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (mCanTouchScroll[1]) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis);
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN:
                mScrollPointerId = e.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (e.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY(actionIndex) + 0.5f);
                break;
            case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    if (isLogAccess()) {
                        print("error", "processing scroll; pointer index for id " + mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    }
                    return false;
                }
                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final int dx = x - mInitialTouchX;
                    final int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    if (willDragging(dx, mCanTouchScroll[0], true)) {
                        mLastTouchX = mInitialTouchX + mTouchSlop * (dx < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (willDragging(dy, mCanTouchScroll[1], false)) {
                        mLastTouchY = mInitialTouchY + mTouchSlop * (dy < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
            }
            break;
            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
            }
            break;
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.clear();
                stopNestedScroll();
            }
            break;
            case MotionEvent.ACTION_CANCEL: {
                cancelTouch(true);
            }
        }
        return mScrollState == SCROLL_STATE_DRAGGING;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (dispatchOnItemTouch(e)) {
            cancelTouch(true);
            return true;
        }
        if (ignoreSelfTouch(false, e)) {
            cancelTouch(true);
            return super.onTouchEvent(e);
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;
        final MotionEvent vtev = MotionEvent.obtain(e);
        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
            mCanTouchScroll[0] = isTouchScrollHorizontalEnable(true);
            mCanTouchScroll[1] = isTouchScrollVerticalEnable(true);
            mScrollPointerId = e.getPointerId(0);
            mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
            int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
            if (mCanTouchScroll[0]) {
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
            }
            if (mCanTouchScroll[1]) {
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
            }
            startNestedScroll(nestedScrollAxis);
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);
        switch (action) {
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = e.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (e.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY(actionIndex) + 0.5f);
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    if (isLogAccess()) {
                        print("error", "processing scroll; pointer index for id " + mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    }
                    return false;
                }
                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;
                if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;
                    if (willDragging(dx, mCanTouchScroll[0], true)) {
                        if (dx > 0) {
                            dx -= mTouchSlop;
                        } else {
                            dx += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (willDragging(dy, mCanTouchScroll[1], false)) {
                        if (dy > 0) {
                            dy -= mTouchSlop;
                        } else {
                            dy += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (startScroll) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];
                    if (scrollByInternal(
                            mCanTouchScroll[0] ? dx : 0,
                            mCanTouchScroll[1] ? dy : 0,
                            vtev)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            break;
            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
            }
            break;
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                final float xvel = mCanTouchScroll[0] ?
                        -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                final float yvel = mCanTouchScroll[1] ?
                        -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
                if (!fling(mLastTouchX - mInitialTouchX, mLastTouchY - mInitialTouchY, (int) xvel, (int) yvel)) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetTouch();
            }
            break;
            case MotionEvent.ACTION_CANCEL: {
                cancelTouch(true);
            }
            break;
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    protected void cancelTouch(boolean resetToIdle) {
        resetTouch();
        if (resetToIdle) {
            setScrollState(SCROLL_STATE_IDLE);
        }
    }

    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        stopNestedScroll();
        releaseGlows();
    }

    protected int formatDuration(int duration) {
        return Math.max(0, Math.min(duration, MAX_SCROLL_DURATION));
    }

    protected Interpolator getDefaultInterpolator() {
        return DefaultInterpolator;
    }

    protected boolean awakenScrollBarsIfNeed() {
        boolean awaken = false;
        boolean horizontal = isHorizontalScrollBarEnabled() && isTouchScrollHorizontalEnable(true);
        boolean vertical = isVerticalScrollBarEnabled() && isTouchScrollVerticalEnable(true);
        if (horizontal || vertical) {
            awaken = awakenScrollBars();
        }
        return awaken;
    }

    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     * It also reports any unused scroll request to the related EdgeEffect.
     *
     * @param x  The amount of horizontal scroll request
     * @param y  The amount of vertical scroll request
     * @param ev The originating MotionEvent, or null if not from a touch event.
     * @return Whether any scroll was consumed in either direction.
     */
    boolean scrollByInternal(int x, int y, MotionEvent ev) {
        int unconsumedX = 0, unconsumedY = 0;
        int consumedX = 0, consumedY = 0;
        if (x != 0) {
            consumedX = willScrollHorizontal(x);
            unconsumedX = x - consumedX;
        }
        if (y != 0) {
            consumedY = willScrollVertical(y);
            unconsumedY = y - consumedY;
        }
        if (consumedX != 0 || consumedY != 0) {
            scrollBy(consumedX, consumedY);
        }
        if (dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, mScrollOffset)) {
            mLastTouchX -= mScrollOffset[0];
            mLastTouchY -= mScrollOffset[1];
            if (ev != null) {
                ev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
            }
            mNestedOffsets[0] += mScrollOffset[0];
            mNestedOffsets[1] += mScrollOffset[1];
        } else if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (ev != null) {
                pullGlows(ev.getX(), unconsumedX, ev.getY(), unconsumedY);
            }
            considerReleasingGlowsOnScroll(x, y);
        }
        awakenScrollBarsIfNeed();
        return consumedX != 0 || consumedY != 0;
    }

    public boolean fling(int velocityX, int velocityY) {
        return fling(0, 0, velocityX, velocityY);
    }

    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second.
     * If the velocity given is below the system-defined minimum this method will return false
     * and no fling will occur.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     * @return true if the fling was started, false if the velocity was too low to fling or
     */
    protected boolean fling(int movedX, int movedY, int velocityX, int velocityY) {
        if (!ignoreSelfFling(velocityX, velocityY)) {
            final boolean canScrollHorizontal = isTouchScrollHorizontalEnable(true);
            final boolean canScrollVertical = isTouchScrollVerticalEnable(true);
            if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
                velocityX = 0;
            }
            if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
                velocityY = 0;
            }
            if ((velocityX != 0 || velocityY != 0) && !dispatchNestedPreFling(velocityX, velocityY)) {
                final boolean canScroll = canScrollHorizontal || canScrollVertical;
                dispatchNestedFling(velocityX, velocityY, canScroll);
                if (canScroll) {
                    velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
                    velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
                    if (isDevLogAccess()) {
                        printDev("fling", String.format("velocityX=%d,velocityY=%d,scrollX,scrollY=%d,rangeX=%d,rangeY=%d", velocityX, velocityY, getScrollX(), getScrollY(), getHorizontalScrollRange(), getVerticalScrollRange()));
                    }
                    mFlingScroller.fling(velocityX, velocityY);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        if (mScrollListener != null) {
            mScrollListener.onScrollChanged(l, t, ol, ot);
        }
    }

    protected void onScrollStateChanged(int newState, int prevState) {
    }

    public void scrollTo(int x, int y, int duration) {
        scrollTo(x, y, duration, getDefaultInterpolator());
    }

    public void scrollTo(int x, int y, int duration, Interpolator interpolator) {
        if (isAttachLayoutFinished()) {
            mScrollInfo.left = 0 << 31;
            x = Math.max(0, Math.min(getHorizontalScrollRange(), x));
            y = Math.max(0, Math.min(getVerticalScrollRange(), y));
            int dx = x - getScrollX(), dy = y - getScrollY();
            if (dx != 0 || dy != 0) {
                if (duration >= 0) {
                    mFlingScroller.smoothScrollBy(dx, dy, duration < 0 ? 0 : duration, interpolator);
                } else {
                    mFlingScroller.smoothScrollBy(dx, dy, interpolator);
                }
            }
        } else {
            mScrollInfo.left = (1 << 31) | ((duration < 0 ? 1 : 0) << 30) | ((duration < 0 ? -duration : duration) & 0x3FFFFFFF);
            mScrollInfo.top = 0;
            mScrollInfo.right = (1 << 31) | ((x < 0 ? 1 : 0) << 30) | ((x < 0 ? -x : x) & 0x3FFFFFFF);
            mScrollInfo.bottom = (1 << 31) | ((y < 0 ? 1 : 0) << 30) | ((y < 0 ? -y : y) & 0x3FFFFFFF);
        }
        mInterpolator = interpolator;
    }

    public void scrollToItem(int index, int duration, boolean centerInParent) {
        scrollToItem(index, duration, 0, 0, centerInParent);
    }

    protected void scrollToItem(int index, int duration, int x, int y, boolean centerInParent) {
        scrollToItem(index, duration, x, y, isTouchScrollHorizontalEnable(false), isTouchScrollVerticalEnable(false), centerInParent);
    }

    protected void scrollToItem(int index, int duration, int x, int y, boolean okX, boolean okY, boolean centerInParent) {
        mScrollInfo.left = (1 << 31) | ((duration < 0 ? 1 : 0) << 30) | ((duration < 0 ? -duration : duration) & 0x3FFFFFFF);
        mScrollInfo.top = ((centerInParent ? 1 : 0) << 31) | ((index < 0 ? 1 : 0) << 30) | ((index < 0 ? -index : index) & 0x3FFFFFFF);
        mScrollInfo.right = ((okX ? 1 : 0) << 31) | ((x < 0 ? 1 : 0) << 30) | ((x < 0 ? -x : x) & 0x3FFFFFFF);
        mScrollInfo.bottom = ((okY ? 1 : 0) << 31) | ((y < 0 ? 1 : 0) << 30) | ((y < 0 ? -y : y) & 0x3FFFFFFF);
        if (isAttachLayoutFinished()) {
            View child =index>=0? getItemView(index):null;
            if(!(child!=null&&child.isLayoutRequested()&&isLayoutRequested())){
                scrollToItem(mScrollInfo, false);
            }
        }
    }

    private void scrollToItem(Rect scrollInfo, boolean afterLayout) {
        if (0x80000000 == (0x80000000 & mScrollInfo.left)) {
            int duration = (mScrollInfo.left & 0x3FFFFFFF) * (0x40000000 == (0x40000000 & mScrollInfo.left) ? -1 : 1);
            int index = (mScrollInfo.top & 0x3FFFFFFF) * (0x40000000 == (0x40000000 & mScrollInfo.top) ? -1 : 1);
            int offsetX = (mScrollInfo.right & 0x3FFFFFFF) * (0x40000000 == (0x40000000 & mScrollInfo.right) ? -1 : 1);
            int offsetY = (mScrollInfo.bottom & 0x3FFFFFFF) * (0x40000000 == (0x40000000 & mScrollInfo.bottom) ? -1 : 1);
            if (index < 0) {
                scrollTo(offsetX, offsetY, duration, mInterpolator);
            } else {
                View child = getItemView(index);
                if (child != null) {
                    int x = getScrollX(), y = getScrollY();
                    boolean centerInParent = 0x80000000 == (0x80000000 & mScrollInfo.top);
                    if (0x80000000 == (0x80000000 & mScrollInfo.right)) {
                        x = offsetX(child, centerInParent, true) + offsetX;
                    }
                    if (0x80000000 == (0x80000000 & mScrollInfo.bottom)) {
                        y = offsetY(child, centerInParent, true) + offsetY;
                    }
                    scrollTo(x, y, duration, mInterpolator);
                }
            }
        }
        mScrollInfo.left = 0 << 31;
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }
    //end:compute scroll information.

    //start:EdgeEffect
    private void ensureLeftGlow() {
        if (mLeftGlow == null) {
            mLeftGlow = new EdgeEffectCompat(getContext());
            if (getClipToPadding()) {
                mLeftGlow.setSize(getHeightWithoutPadding(), getWidthWithoutPadding());
            } else {
                mLeftGlow.setSize(getMeasuredHeight(), getMeasuredWidth());
            }
        }
    }

    private void ensureRightGlow() {
        if (mRightGlow == null) {
            mRightGlow = new EdgeEffectCompat(getContext());
            if (getClipToPadding()) {
                mRightGlow.setSize(getHeightWithoutPadding(), getWidthWithoutPadding());
            } else {
                mRightGlow.setSize(getMeasuredHeight(), getMeasuredWidth());
            }
        }
    }

    private void ensureTopGlow() {
        if (mTopGlow == null) {
            mTopGlow = new EdgeEffectCompat(getContext());
            if (getClipToPadding()) {
                mTopGlow.setSize(getWidthWithoutPadding(), getHeightWithoutPadding());
            } else {
                mTopGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    private void ensureBottomGlow() {
        if (mBottomGlow == null) {
            mBottomGlow = new EdgeEffectCompat(getContext());
            if (getClipToPadding()) {
                mBottomGlow.setSize(getWidthWithoutPadding(), getHeightWithoutPadding());
            } else {
                mBottomGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    protected void invalidateGlows() {
        mLeftGlow = mRightGlow = mTopGlow = mBottomGlow = null;
    }

    protected void pullGlows(float x, float overscrollX, float y, float overscrollY) {
        if (mEdgeEffectEnable) {
            boolean invalidate = false;
            if (overscrollX < 0) {
                ensureLeftGlow();
                if (mLeftGlow.onPull(-overscrollX / getWidth(), 1f - y / getHeight())) {
                    invalidate = true;
                }
            } else if (overscrollX > 0) {
                ensureRightGlow();
                if (mRightGlow.onPull(overscrollX / getWidth(), y / getHeight())) {
                    invalidate = true;
                }
            }
            if (overscrollY < 0) {
                ensureTopGlow();
                if (mTopGlow.onPull(-overscrollY / getHeight(), x / getWidth())) {
                    invalidate = true;
                }
            } else if (overscrollY > 0) {
                ensureBottomGlow();
                if (mBottomGlow.onPull(overscrollY / getHeight(), 1f - x / getWidth())) {
                    invalidate = true;
                }
            }
            if (invalidate || overscrollX != 0 || overscrollY != 0) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    protected void considerReleasingGlowsOnScroll(int dx, int dy) {
        if (mEdgeEffectEnable) {
            boolean needsInvalidate = false;
            if (mLeftGlow != null && !mLeftGlow.isFinished() && dx > 0) {
                needsInvalidate = mLeftGlow.onRelease();
            }
            if (mRightGlow != null && !mRightGlow.isFinished() && dx < 0) {
                needsInvalidate |= mRightGlow.onRelease();
            }
            if (mTopGlow != null && !mTopGlow.isFinished() && dy > 0) {
                needsInvalidate |= mTopGlow.onRelease();
            }
            if (mBottomGlow != null && !mBottomGlow.isFinished() && dy < 0) {
                needsInvalidate |= mBottomGlow.onRelease();
            }
            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    protected void releaseGlows() {
        if (mEdgeEffectEnable) {
            boolean needsInvalidate = false;
            if (mLeftGlow != null) needsInvalidate = mLeftGlow.onRelease();
            if (mTopGlow != null) needsInvalidate |= mTopGlow.onRelease();
            if (mRightGlow != null) needsInvalidate |= mRightGlow.onRelease();
            if (mBottomGlow != null) needsInvalidate |= mBottomGlow.onRelease();
            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    protected void absorbGlows(int velocityX, int velocityY) {
        if (mEdgeEffectEnable) {
            if (velocityX < 0) {
                ensureLeftGlow();
                mLeftGlow.onAbsorb(-velocityX);
            } else if (velocityX > 0) {
                ensureRightGlow();
                mRightGlow.onAbsorb(velocityX);
            }
            if (velocityY < 0) {
                ensureTopGlow();
                mTopGlow.onAbsorb(-velocityY);
            } else if (velocityY > 0) {
                ensureBottomGlow();
                mBottomGlow.onAbsorb(velocityY);
            }
            if (velocityX != 0 || velocityY != 0) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }
    //end:EdgeEffect

    //start:NestedScrollingChild
    private NestedScrollingChildHelper getScrollingChildHelper() {
        if (mScrollingChildHelper == null) {
            mScrollingChildHelper = new NestedScrollingChildHelper(this);
        }
        return mScrollingChildHelper;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        getScrollingChildHelper().setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return getScrollingChildHelper().isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return getScrollingChildHelper().startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        getScrollingChildHelper().stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return getScrollingChildHelper().hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return getScrollingChildHelper().dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return getScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return getScrollingChildHelper().dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return getScrollingChildHelper().dispatchNestedPreFling(velocityX, velocityY);
    }
    //end:NestedScrollingChild

    protected final static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
            return 0;
        }
        if ((my + n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
            return child - my;
        }
        return n;
    }

    class FlingScroller implements Runnable {
        private int mLastFlingX;
        private int mLastFlingY;
        private ScrollerCompat mScroller;
        Interpolator mInterpolator = getDefaultInterpolator();
        // When set to true, postOnAnimation callbacks are delayed until the run method completes
        private boolean mEatRunOnAnimationRequest = false;
        // Tracks if postAnimationCallback should be re-attached when it is done
        private boolean mReSchedulePostAnimationCallback = false;

        public FlingScroller() {
            mScroller = ScrollerCompat.create(getContext(), getDefaultInterpolator());
        }

        @Override
        public void run() {
            disableRunOnAnimationRequests();
            // keep a local reference so that if it is changed during onAnimation method, it won't
            // cause unexpected behaviors
            final ScrollerCompat scroller = mScroller;
            if (scroller.computeScrollOffset()) {
                final int x = scroller.getCurrX();
                final int y = scroller.getCurrY();
                final int dx = x - mLastFlingX;
                final int dy = y - mLastFlingY;
                int hresult = 0;
                int vresult = 0;
                mLastFlingX = x;
                mLastFlingY = y;
                int overscrollX = 0, overscrollY = 0;
                if (dx != 0) {
                    hresult = willScrollHorizontal(dx);
                    overscrollX = dx - hresult;
                }
                if (dy != 0) {
                    vresult = willScrollVertical(dy);
                    overscrollY = dy - vresult;
                }
                if (hresult != 0 || vresult != 0) {
                    scrollBy(hresult, vresult);
                }
                if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
                    considerReleasingGlowsOnScroll(dx, dy);
                }
                if (overscrollX != 0 || overscrollY != 0) {
                    final int vel = (int) scroller.getCurrVelocity();
                    int velX = 0;
                    if (overscrollX != x) {
                        velX = overscrollX < 0 ? -vel : overscrollX > 0 ? vel : 0;
                    }
                    int velY = 0;
                    if (overscrollY != y) {
                        velY = overscrollY < 0 ? -vel : overscrollY > 0 ? vel : 0;
                    }
                    if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
                        absorbGlows(velX, velY);
                    }
                    if ((velX != 0 || overscrollX == x || scroller.getFinalX() == 0) &&
                            (velY != 0 || overscrollY == y || scroller.getFinalY() == 0)) {
                        scroller.abortAnimation();
                    }
                }
                awakenScrollBarsIfNeed();
                final boolean fullyConsumedVertical = dy != 0 && isTouchScrollVerticalEnable(true)
                        && vresult == dy;
                final boolean fullyConsumedHorizontal = dx != 0 && isTouchScrollHorizontalEnable(true)
                        && hresult == dx;
                final boolean fullyConsumedAny = (dx == 0 && dy == 0) || fullyConsumedHorizontal
                        || fullyConsumedVertical;
                if (scroller.isFinished() || !fullyConsumedAny) {
                    if (isDevLogAccess()) {
                        printDev("fling", "scroller finished or full consumed");
                    }
                    setScrollState(SCROLL_STATE_IDLE); // setting state to idle will stop this.
                } else {
                    postOnAnimation();
                }
            }
            enableRunOnAnimationRequests();
        }

        private void disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false;
            mEatRunOnAnimationRequest = true;
        }

        private void enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false;
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation();
            }
        }

        void postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true;
            } else {
                removeCallbacks(this);
                ViewCompat.postOnAnimation(ScrollLayout.this, this);
            }
        }

        public void fling(int velocityX, int velocityY) {
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.fling(0, 0, velocityX, velocityY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }

        public void fling(int velocityX, int velocityY, Interpolator interpolator) {
            if (mInterpolator != interpolator) {
                mInterpolator = interpolator;
                mScroller = ScrollerCompat.create(getContext(), interpolator);
            }
            fling(velocityX, velocityY);
        }

        public void smoothScrollBy(int dx, int dy) {
            smoothScrollBy(dx, dy, 0, 0);
        }

        public void smoothScrollBy(int dx, int dy, int vx, int vy) {
            smoothScrollBy(dx, dy, computeScrollDuration(dx, dy, vx, vy));
        }

        private float distanceInfluenceForSnapDuration(float f) {
            f -= 0.5f; // center the values about 0.
            f *= 0.3f * Math.PI / 2.0f;
            return (float) Math.sin(f);
        }

        private int computeScrollDuration(int dx, int dy, int vx, int vy) {
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            final boolean horizontal = absDx > absDy;
            final int velocity = (int) Math.sqrt(vx * vx + vy * vy);
            final int delta = (int) Math.sqrt(dx * dx + dy * dy);
            final int containerSize = horizontal ? getWidth() : getHeight();
            final int halfContainerSize = containerSize / 2;
            final float distanceRatio = Math.min(1.f, 1.f * delta / containerSize);
            final float distance = halfContainerSize + halfContainerSize *
                    distanceInfluenceForSnapDuration(distanceRatio);

            final int duration;
            if (velocity > 0) {
                duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
            } else {
                float absDelta = (float) (horizontal ? absDx : absDy);
                duration = (int) (((absDelta / containerSize) + 1) * 300);
            }
            return formatDuration(duration);
        }

        public void smoothScrollBy(int dx, int dy, int duration) {
            smoothScrollBy(dx, dy, duration, getDefaultInterpolator());
        }

        public void smoothScrollBy(int dx, int dy, Interpolator interpolator) {
            smoothScrollBy(dx, dy, computeScrollDuration(dx, dy, 0, 0),
                    interpolator == null ? getDefaultInterpolator() : interpolator);
        }

        public void smoothScrollBy(int dx, int dy, int duration, Interpolator interpolator) {
            if (mInterpolator != interpolator) {
                mInterpolator = interpolator;
                mScroller = ScrollerCompat.create(getContext(), interpolator);
            }
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.startScroll(0, 0, dx, dy, duration);
            postOnAnimation();
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }
    }

    public interface OnScrollChangeListener {
        void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY);

        void onScrollStateChanged(int state, int oldState);
    }
}