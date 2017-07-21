package com.rexy.widgets.group;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.rexy.widgetlayout.R;

/**
 * <!--水平方向Item 的间距-->
 * <attr name="middleMarginHorizontal" format="dimension"/>
 * <!--垂直方向Item 的间距-->
 * <attr name="middleMarginVertical" format="dimension"/>
 * <p>
 * <!--水平分割线颜色-->
 * <attr name="dividerColorHorizontal" format="color"/>
 * <!--水平分割线宽-->
 * <attr name="dividerWidthHorizontal" format="dimension"/>
 * <p>
 * <!--水平分割线开始padding-->
 * <attr name="dividerPaddingStartHorizontal" format="dimension"/>
 * <!--水平分割线结束padding-->
 * <attr name="dividerPaddingEndHorizontal" format="dimension"/>
 * <p>
 * <!--垂直分割线颜色-->
 * <attr name="dividerColorVertical" format="color"/>
 * <!--垂直分割线宽-->
 * <attr name="dividerWidthVertical" format="dimension"/>
 * <p>
 * <!--垂直分割线开始padding-->
 * <attr name="dividerPaddingStartVertical" format="dimension"/>
 * <!--垂直分割线结束padding-->
 * <attr name="dividerPaddingEndVertical" format="dimension"/>
 * <!--每行内容水平居中-->
 * <attr name="lineCenterHorizontal" format="boolean"/>
 * <!--每行内容垂直居中-->
 * <attr name="lineCenterVertical" format="boolean"/>
 * <p>
 * <!--每一行最少的Item 个数-->
 * <attr name="lineMinItemCount" format="integer"/>
 * <!--每一行最多的Item 个数-->
 * <attr name="lineMaxItemCount" format="integer"/>
 * <!-- 支持weight 属性，前提条件是单行或单列的情况-->
 * <attr name="weightSupport" format="boolean" />
 *
 * @author: rexy
 * @date: 2015-11-27 17:43
 */
public class WrapLayout extends PressViewGroup {
    //每行内容水平居中
    protected boolean mEachLineCenterHorizontal = false;
    //每行内容垂直居中
    protected boolean mEachLineCenterVertical = false;

    protected boolean mSupportWeight = false;

    //每一行最少的Item 个数
    protected int mEachLineMinItemCount = 0;
    //每一行最多的Item 个数
    protected int mEachLineMaxItemCount = 0;

    protected int mContentMaxWidthAccess = 0;
    protected int mWeightSum = 0;
    protected SparseIntArray mLineHeight = new SparseIntArray(2);
    protected SparseIntArray mLineWidth = new SparseIntArray(2);
    protected SparseIntArray mLineItemCount = new SparseIntArray(2);
    protected SparseIntArray mLineEndIndex = new SparseIntArray(2);
    protected SparseArray<View> mWeightView = new SparseArray(2);

    public WrapLayout(Context context) {
        super(context);
        init(context, null);
    }

    public WrapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WrapLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public WrapLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDividerMargin = DividerMargin.from(context, attrs);
        TypedArray attr = attrs == null ? null : context.obtainStyledAttributes(attrs, R.styleable.WrapLayout);
        if (attr != null) {
            mEachLineMinItemCount = attr.getInt(R.styleable.WrapLayout_lineMinItemCount, mEachLineMinItemCount);
            mEachLineMaxItemCount = attr.getInt(R.styleable.WrapLayout_lineMaxItemCount, mEachLineMaxItemCount);
            mEachLineCenterHorizontal = attr.getBoolean(R.styleable.WrapLayout_lineCenterHorizontal, mEachLineCenterHorizontal);
            mEachLineCenterVertical = attr.getBoolean(R.styleable.WrapLayout_lineCenterVertical, mEachLineCenterVertical);
            mSupportWeight = attr.getBoolean(R.styleable.WrapLayout_weightSupport, mSupportWeight);
            attr.recycle();
        }
    }

    private boolean ifNeedNewLine(View child, int attemptWidth, int countInLine) {
        boolean needLine = false;
        if (countInLine > 0) {
            if (countInLine >= mEachLineMinItemCount) {
                if (mEachLineMaxItemCount > 0 && countInLine >= mEachLineMaxItemCount) {
                    needLine = true;
                } else {
                    if (attemptWidth > mContentMaxWidthAccess) {
                        needLine = !(mSupportWeight && mEachLineMinItemCount <= 0 && mEachLineMaxItemCount != 1);
                    }
                }
            }
        }
        return needLine;
    }

    @Override
    protected void dispatchMeasure(int widthMeasureSpecNoPadding, int heightMeasureSpecNoPadding, int maxSelfWidthNoPadding, int maxSelfHeightNoPadding) {
        final int contentMarginHorizontal = mDividerMargin.getContentMarginLeft() + mDividerMargin.getContentMarginRight();
        final int contentMarginVertical = mDividerMargin.getContentMarginTop() + mDividerMargin.getContentMarginBottom();
        maxSelfWidthNoPadding -= contentMarginHorizontal;
        maxSelfHeightNoPadding -= contentMarginVertical;
        widthMeasureSpecNoPadding = MeasureSpec.makeMeasureSpec(maxSelfWidthNoPadding, MeasureSpec.getMode(widthMeasureSpecNoPadding));
        heightMeasureSpecNoPadding = MeasureSpec.makeMeasureSpec(maxSelfHeightNoPadding, MeasureSpec.getMode(heightMeasureSpecNoPadding));
        final boolean ignoreBeyondWidth = true;
        final int childCount = getChildCount();
        mLineHeight.clear();
        mLineEndIndex.clear();
        mLineItemCount.clear();
        mLineWidth.clear();
        mContentMaxWidthAccess = maxSelfWidthNoPadding;
        mWeightSum = 0;
        int currentLineIndex = 0;
        int currentLineMaxWidth = 0;
        int currentLineMaxHeight = 0;
        int currentLineItemCount = 0;

        int contentWidth = 0, contentHeight = 0, childState = 0, lastMeasureIndex = 0;
        int middleMarginHorizontal = mDividerMargin.getContentMarginHorizontal();
        int middleMarginVertical = mDividerMargin.getContentMarginVertical();

        final boolean supportWeight = mSupportWeight && ((mEachLineMaxItemCount == 1) || (mEachLineMinItemCount >= childCount || mEachLineMinItemCount <= 0));
        mWeightView.clear();
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if (skipChild(child)) continue;
            WrapLayout.LayoutParams params = (WrapLayout.LayoutParams) child.getLayoutParams();
            if (params.weight > 0) {
                if (!mSupportWeight) {
                    throw new IllegalArgumentException("use weight feature,should setSupportWeight true ");
                }
                mWeightSum += params.weight;
                if (supportWeight) {
                    mWeightView.put(childIndex, child);
                    continue;
                }
            }
            int childMarginHorizontal = params.leftMargin + params.rightMargin;
            int childMarginVertical = params.topMargin + params.bottomMargin;
            params.measure(child, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, childMarginHorizontal, childMarginVertical + contentHeight);
            int childWidthWithMargin = child.getMeasuredWidth() + childMarginHorizontal;
            int childHeightWithMargin = child.getMeasuredHeight() + childMarginVertical;
            childState |= child.getMeasuredState();
            lastMeasureIndex = childIndex;
            if (ifNeedNewLine(child, childWidthWithMargin + currentLineMaxWidth + middleMarginHorizontal, currentLineItemCount)) {
                if (ignoreBeyondWidth || currentLineMaxWidth <= mContentMaxWidthAccess) {
                    contentWidth = Math.max(contentWidth, currentLineMaxWidth);
                }
                if (middleMarginVertical > 0) {
                    contentHeight += middleMarginVertical;
                }
                contentHeight += currentLineMaxHeight;
                mLineWidth.put(currentLineIndex, currentLineMaxWidth);
                mLineHeight.put(currentLineIndex, currentLineMaxHeight);
                mLineItemCount.put(currentLineIndex, currentLineItemCount);
                mLineEndIndex.put(currentLineIndex, childIndex - 1);
                currentLineIndex += 1;
                currentLineItemCount = 1;
                currentLineMaxWidth = childWidthWithMargin;
                currentLineMaxHeight = childHeightWithMargin;
            } else {
                if (currentLineItemCount > 0 && middleMarginHorizontal > 0) {
                    currentLineMaxWidth += middleMarginHorizontal;
                }
                currentLineItemCount = currentLineItemCount + 1;
                currentLineMaxWidth += childWidthWithMargin;
                if (!ignoreBeyondWidth && currentLineMaxWidth <= mContentMaxWidthAccess) {
                    contentWidth = Math.max(contentWidth, currentLineMaxWidth);
                }
                currentLineMaxHeight = Math.max(currentLineMaxHeight, childHeightWithMargin);
            }
        }
        if (currentLineItemCount > 0) {
            if (ignoreBeyondWidth || currentLineMaxWidth <= mContentMaxWidthAccess) {
                contentWidth = Math.max(contentWidth, currentLineMaxWidth);
            }
            contentHeight += currentLineMaxHeight;
            mLineWidth.put(currentLineIndex, currentLineMaxWidth);
            mLineHeight.put(currentLineIndex, currentLineMaxHeight);
            mLineItemCount.put(currentLineIndex, currentLineItemCount);
            mLineEndIndex.put(currentLineIndex, lastMeasureIndex);
        }
        int weightListSize = supportWeight ? mWeightView.size() : 0;
        if (weightListSize > 0) {
            boolean needAdjustMargin = mLineItemCount.size() == 0;
            boolean vertical = mEachLineMaxItemCount == 1;
            int remain, adjustMargin;
            int widthMeasureSpec = widthMeasureSpecNoPadding, heightMeasureSpec = heightMeasureSpecNoPadding;
            if (vertical) {
                adjustMargin = (needAdjustMargin ? weightListSize - 1 : weightListSize) * middleMarginVertical;
                remain = maxSelfHeightNoPadding - contentHeight - contentMarginVertical - adjustMargin;
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(remain, MeasureSpec.getMode(heightMeasureSpec));
            } else {
                adjustMargin = (needAdjustMargin ? weightListSize - 1 : weightListSize) * middleMarginHorizontal;
                remain = maxSelfWidthNoPadding - contentWidth - contentMarginHorizontal - adjustMargin;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(remain, MeasureSpec.getMode(widthMeasureSpec));
            }
            if (remain > mWeightView.size()) {
                int[] r = new int[2];
                adjustMeasureWithWeight(widthMeasureSpec, heightMeasureSpec, r, vertical);
                if (vertical) {
                    contentHeight += (r[1] + adjustMargin);
                    contentWidth = Math.max(contentWidth, r[0]);
                } else {
                    contentWidth += (r[0] + adjustMargin);
                    contentHeight = Math.max(contentHeight, r[1]);
                    mLineWidth.put(0,mLineWidth.get(0)+adjustMargin);
                }
            }
            mWeightView.clear();
        }
        setContentSize(contentWidth + contentMarginHorizontal, contentHeight + contentMarginVertical);
        setMeasureState(childState);
    }


    private void adjustMeasureWithWeight(int widthMeasureSpec, int heightMeasureSpec, int[] r, boolean vertical) {
        int size = mWeightView.size();
        float sizeAccess = MeasureSpec.getSize(vertical ? heightMeasureSpec : widthMeasureSpec);
        for (int i = 0; i < size; i++) {
            int childIndex = mWeightView.keyAt(i);
            View child = mWeightView.get(childIndex);
            WrapLayout.LayoutParams params = (LayoutParams) child.getLayoutParams();
            int marginHorizontal = params.getMarginHorizontal();
            int marginVertical = params.getMarginVertical();
            int childWidthMeasureSpec, childHeightMeasureSpec;
            if (vertical) {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, marginHorizontal, params.width);
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec((int) (((sizeAccess * params.weight) / mWeightSum) - marginVertical), MeasureSpec.EXACTLY);
            } else {
                childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, marginVertical, params.height);
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec((int) (((sizeAccess * params.weight) / mWeightSum) - marginHorizontal), MeasureSpec.EXACTLY);
            }
            params.measure(child, childWidthMeasureSpec, childHeightMeasureSpec);
            insertMeasureInfo(child.getMeasuredWidth() + marginHorizontal, child.getMeasuredHeight() + marginVertical, childIndex, r, vertical);
        }
    }

    private void insertMeasureInfo(int itemWidth, int itemHeight, int childIndex, int[] r, boolean vertical) {
        if (vertical) {
            r[0] = Math.max(r[0], itemWidth);
            r[1] += itemHeight;
            int betterLineIndex = -1;
            int lineSize = mLineEndIndex.size();
            for (int lineIndex = lineSize - 1; lineIndex >= 0; lineIndex--) {
                int line = mLineEndIndex.keyAt(lineIndex);
                if (childIndex > mLineEndIndex.get(line)) {
                    betterLineIndex = lineIndex;
                    break;
                }
            }
            betterLineIndex += 1;
            int goodLine = betterLineIndex < mLineEndIndex.size() ? mLineEndIndex.keyAt(betterLineIndex) : betterLineIndex;
            for (int lineIndex = lineSize - 1; lineIndex >= betterLineIndex; lineIndex--) {
                int line = mLineEndIndex.keyAt(lineIndex);
                mLineEndIndex.put(line + 1, mLineEndIndex.get(line));
                mLineItemCount.put(line + 1, mLineItemCount.get(line));
                mLineWidth.put(line + 1, mLineWidth.get(line));
                mLineHeight.put(line + 1, mLineHeight.get(line));
            }
            mLineEndIndex.put(goodLine, childIndex);
            mLineItemCount.put(goodLine, 1);
            mLineWidth.put(goodLine, itemWidth);
            mLineHeight.put(goodLine, itemHeight);
        } else {
            r[0] += itemWidth;
            r[1] = Math.max(r[1], itemHeight);
            mLineEndIndex.put(0, Math.max(mLineEndIndex.get(0), childIndex));
            mLineItemCount.put(0, mLineItemCount.get(0) + 1);
            mLineHeight.put(0, Math.max(mLineHeight.get(0), itemHeight));
            mLineWidth.put(0, mLineWidth.get(0) + itemWidth);
        }
    }

    @Override
    protected void dispatchLayout(int contentLeft, int contentTop, int paddingLeft, int paddingTop, int selfWidthNoPadding, int selfHeightNoPadding) {
        final int lineCount = mLineEndIndex.size(), gravity = getGravity();
        final boolean lineVertical = mEachLineCenterVertical || ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL && lineCount == 1);
        final boolean lineHorizontal = mEachLineCenterHorizontal || ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL && lineCount == 1);
        final int middleMarginHorizontal = mDividerMargin.getContentMarginHorizontal();
        final int middleMarginVertical = mDividerMargin.getContentMarginVertical();
        final int contentMarginLeft = mDividerMargin.getContentMarginLeft();
        final int contentMarginTop = mDividerMargin.getContentMarginTop();
        final int contentWidthNoMargin = getContentWidth() - mDividerMargin.getContentMarginLeft() - mDividerMargin.getContentMarginRight();
        int lineEndIndex, lineMaxHeight, childIndex = 0, lineTop = contentTop + contentMarginTop;
        int childLeft, childTop, childRight, childBottom;
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            lineEndIndex = mLineEndIndex.get(lineIndex);
            lineMaxHeight = mLineHeight.get(lineIndex);
            childLeft = contentLeft + contentMarginLeft;
            if (lineHorizontal) {
                childLeft += (contentWidthNoMargin - mLineWidth.get(lineIndex)) / 2;
            }
            for (; childIndex <= lineEndIndex; childIndex++) {
                final View child = getChildAt(childIndex);
                if (skipChild(child)) continue;
                WrapLayout.LayoutParams params = (WrapLayout.LayoutParams) child.getLayoutParams();
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                childLeft += params.leftMargin;
                childTop = getContentStartV(lineTop, lineTop + lineMaxHeight, childHeight, params.topMargin, params.bottomMargin, lineVertical ? Gravity.CENTER_VERTICAL : params.gravity);
                childRight = childLeft + childWidth;
                childBottom = childTop + childHeight;
                child.layout(childLeft, childTop, childRight, childBottom);
                childLeft = childRight + params.rightMargin;
                if (middleMarginHorizontal > 0) {
                    childLeft += middleMarginHorizontal;
                }
            }
            childIndex = lineEndIndex + 1;
            lineTop += lineMaxHeight;
            if (middleMarginVertical > 0) {
                lineTop += middleMarginVertical;
            }
        }
    }

    @Override
    protected void dispatchDrawAfter(Canvas canvas) {
        boolean dividerHorizontal = mDividerMargin.isVisibleDividerHorizontal(true);
        boolean dividerVertical = mDividerMargin.isVisibleDividerVertical(true);
        if (dividerHorizontal || dividerVertical) {
            final int lineCount = mLineEndIndex.size();
            final int middleMarginHorizontal = mDividerMargin.getContentMarginHorizontal();
            final int middleMarginVertical = mDividerMargin.getContentMarginVertical();
            final int contentMarginTop = mDividerMargin.getContentMarginTop();

            int parentLeft = getPaddingLeft();
            int parentRight = getWidth() - getPaddingRight();

            int lineIndex = 0, childIndex = 0;
            int lineTop = getContentTop() + contentMarginTop, lineBottom;
            for (; lineIndex < lineCount; lineIndex++) {
                int lineEndIndex = mLineEndIndex.get(lineIndex);
                lineBottom = lineTop + mLineHeight.get(lineIndex);
                if (dividerHorizontal && lineIndex != lineCount - 1) {
                    mDividerMargin.drawDividerH(canvas, parentLeft, parentRight, lineBottom + (middleMarginVertical > 0 ? middleMarginVertical / 2 : 0));
                }
                if (dividerVertical && mLineItemCount.get(lineIndex) > 1) {
                    for (; childIndex < lineEndIndex; childIndex++) {
                        final View child = getChildAt(childIndex);
                        if (skipChild(child)) continue;
                        WrapLayout.LayoutParams params = (WrapLayout.LayoutParams) child.getLayoutParams();
                        mDividerMargin.drawDividerV(canvas, lineTop, lineBottom, child.getRight() + params.rightMargin + (middleMarginHorizontal > 0 ? middleMarginHorizontal / 2 : 0));
                    }
                }
                childIndex = lineEndIndex + 1;
                lineTop = lineBottom;
                if (middleMarginVertical > 0) {
                    lineTop += middleMarginVertical;
                }
            }
        }
        super.dispatchDrawAfter(canvas);
    }

    public int getEachLineMinItemCount() {
        return mEachLineMinItemCount;
    }

    public int getEachLineMaxItemCount() {
        return mEachLineMaxItemCount;
    }

    public boolean isEachLineCenterHorizontal() {
        return mEachLineCenterHorizontal;
    }

    public boolean isEachLineCenterVertical() {
        return mEachLineCenterVertical;
    }

    public boolean isSupportWeight() {
        return mSupportWeight;
    }

    public void setSupportWeight(boolean supportWeight) {
        if (mSupportWeight != supportWeight) {
            mSupportWeight = supportWeight;
            if (mWeightSum > 0) {
                requestLayout();
            }
        }
    }

    public void setEachLineMinItemCount(int eachLineMinItemCount) {
        if (mEachLineMinItemCount != eachLineMinItemCount) {
            mEachLineMinItemCount = eachLineMinItemCount;
            requestLayout();
        }
    }

    public void setEachLineMaxItemCount(int eachLineMaxItemCount) {
        if (mEachLineMaxItemCount != eachLineMaxItemCount) {
            mEachLineMaxItemCount = eachLineMaxItemCount;
            requestLayout();
        }
    }

    public void setEachLineCenterHorizontal(boolean eachLineCenterHorizontal) {
        if (mEachLineCenterHorizontal != eachLineCenterHorizontal) {
            mEachLineCenterHorizontal = eachLineCenterHorizontal;
            requestLayout();
        }
    }

    public void setEachLineCenterVertical(boolean eachLineCenterVertical) {
        if (mEachLineCenterVertical != eachLineCenterVertical) {
            mEachLineCenterVertical = eachLineCenterVertical;
            requestLayout();
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof WrapLayout.LayoutParams;
    }

    @Override
    public WrapLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new WrapLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected WrapLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof MarginLayoutParams) {
            return new WrapLayout.LayoutParams((MarginLayoutParams) p);
        }
        return new WrapLayout.LayoutParams(p);
    }

    @Override
    protected WrapLayout.LayoutParams generateDefaultLayoutParams() {
        return new WrapLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static class LayoutParams extends BaseViewGroup.LayoutParams {
        public float weight = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = attrs == null ? null : c.obtainStyledAttributes(attrs, new int[]{android.R.attr.layout_weight});
            if (a != null) {
                weight = a.getFloat(0, weight);
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            if (source instanceof WrapLayout.LayoutParams) {
                weight = ((WrapLayout.LayoutParams) source).weight;
            }
            if (source instanceof LinearLayout.LayoutParams) {
                weight = ((LinearLayout.LayoutParams) source).weight;
            }
        }
    }

}
