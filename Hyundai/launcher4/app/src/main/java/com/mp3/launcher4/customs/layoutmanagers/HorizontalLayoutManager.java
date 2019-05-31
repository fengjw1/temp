package com.mp3.launcher4.customs.layoutmanagers;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.mp3.launcher4.R;
import com.mp3.launcher4.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longzj
 */
public class HorizontalLayoutManager extends RecyclerView.LayoutManager {

    private final static int ADD_LEFT = 0;
    private final static int ADD_RIGHT = 1;
    private final String TAG = getClass().getSimpleName();
    private int mBaseOffset;
    private OrientationHelper mOrientationHelper;
    private List<Rect> mRectList;

    private int mTotalScrollOffset;

    private int mLeftEdgeOffset;
    private int mItemWidth = 0;

    public HorizontalLayoutManager(Context context) {
        mRectList = new ArrayList<>();
        mTotalScrollOffset = 0;
        mLeftEdgeOffset = ImageUtils.dp2Px(context.getApplicationContext(), 180);
        mBaseOffset = ImageUtils.dp2Px(context.getApplicationContext(),
                context.getResources().getDimensionPixelSize(R.dimen.home_start_margin));
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (getItemCount() <= 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (state.isPreLayout()) {
            return;
        }
        initHelper();
        detachAndScrapAttachedViews(recycler);
        mRectList.clear();
        int count = getItemCount();
        int parentHeight = getHeight();
        int leftOffset = mOrientationHelper.getStartAfterPadding() - mTotalScrollOffset;
        for (int index = 0; index < count; index++) {
            View child = recycler.getViewForPosition(index);
            addView(child);
            measureChildWithMargins(child, 0, 0);
            int width = getDecoratedMeasuredWidth(child);
            int height = getDecoratedMeasuredHeight(child);
            int top = getPaddingTop();
            int left = leftOffset;
            int right = left + width;
            int bottom = top + height;
            if (left >= mOrientationHelper.getEndAfterPadding()) {
                removeAndRecycleView(child, recycler);
                break;
            }
            mRectList.add(new Rect(left, top, right, bottom));
            detachAndScrapView(child, recycler);
            leftOffset = right;
            mItemWidth = width;
        }
        fillChildren(recycler);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        dx = reviseScrollOffset(dx);
        recycleViews(recycler, dx);
        fillChild(recycler, dx, state);
        offsetChildrenHorizontal(-dx);
        resetViewsAlpha();
        mTotalScrollOffset += dx;
        return dx;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {

        int count = getItemCount();
        int fromPos = getPosition(focused);

        switch (direction) {
            case View.FOCUS_LEFT:
                fromPos--;
                break;
            default:
                break;
        }

        if (fromPos < 0) {
            return focused;
        }
        return super.onInterceptFocusSearch(focused, direction);
    }

    public int getTotalScrollOffset() {
        return mTotalScrollOffset;
    }

    private void recycleViews(RecyclerView.Recycler recycler, int dx) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (dx > 0) {
                int childEnd = mOrientationHelper.getDecoratedEnd(child);
                int parentStart = mOrientationHelper.getStartAfterPadding() - mBaseOffset;
                if (childEnd - dx <= parentStart) {
                    removeAndRecycleView(child, recycler);
                }
            } else {
                int childStart = mOrientationHelper.getDecoratedStart(child);
                int parentEnd = mOrientationHelper.getEndAfterPadding();
                if (childStart - dx >= parentEnd) {
                    removeAndRecycleView(child, recycler);
                }
            }
        }
    }

    private void fillChild(RecyclerView.Recycler recycler, int dx, RecyclerView.State state) {
        if (dx > 0) {
            fillRightChild(recycler, dx, state);
        } else {
            fillLeftChild(recycler, dx);
        }
    }

    private void fillRightChild(RecyclerView.Recycler recycler, int dx, RecyclerView.State state) {
        View lastView = getChildAt(getChildCount() - 1);
        if (lastView == null) {
            return;
        }
        int position = getPosition(lastView) + 1;
        int offset = mOrientationHelper.getDecoratedEnd(lastView);
        if (offset - dx < mOrientationHelper.getEndAfterPadding()) {
            if (position < state.getItemCount()) {
                layoutScrap(recycler, position, offset, ADD_RIGHT);
            }
        }
    }

    private void fillLeftChild(RecyclerView.Recycler recycler, int dx) {
        View firstView = getChildAt(0);
        if (firstView == null) {
            return;
        }
        int position = getPosition(firstView) - 1;
        int offset = mOrientationHelper.getDecoratedStart(firstView);
        if (offset - dx > mOrientationHelper.getStartAfterPadding() - mBaseOffset) {
            if (position >= 0) {
                layoutScrap(recycler, position, offset, ADD_LEFT);
            }
        }
    }

    private void layoutScrap(RecyclerView.Recycler recycler, int position, int offset, int direction) {

        View child = recycler.getViewForPosition(position);
        if (direction == ADD_RIGHT) {
            addView(child);
        } else if (direction == ADD_LEFT) {
            addView(child, 0);
        }

        int parentHeight = getHeight();
        measureChildWithMargins(child, 0, 0);
        int width = getDecoratedMeasuredWidth(child);
        int height = getDecoratedMeasuredHeight(child);
        int top = getPaddingTop();
        int right = offset + width;
        int bottom = top + height;
        if (direction == ADD_RIGHT) {
            layoutDecorated(child, offset, top, right, bottom);
            child.setAlpha(calculateViewAlpha(offset, width));
        } else if (direction == ADD_LEFT) {
            layoutDecorated(child, offset - width, top, offset, bottom);
            child.setAlpha(calculateViewAlpha(offset - width, width));
        }
    }

    private void initHelper() {
        if (mOrientationHelper == null) {
            mOrientationHelper = OrientationHelper.createHorizontalHelper(this);
        }
    }

    private int reviseScrollOffset(int dx) {
        int itemCount = getItemCount();
        int count = getChildCount();
        int parentLeft = mOrientationHelper.getStartAfterPadding();
        View lastView = getChildAt(count - 1);
        int lastPos = getPosition(lastView);
        if (lastPos == itemCount - 1 && dx > 0) {
            int left = mOrientationHelper.getDecoratedStart(lastView);
            if (left - dx < mOrientationHelper.getStartAfterPadding()) {
                return left - mOrientationHelper.getStartAfterPadding();
            }
        }

        View firstView = getChildAt(0);
        int firstPos = getPosition(firstView);
        if (firstPos == 0 && dx < 0) {
            int left = mOrientationHelper.getDecoratedStart(firstView);
            if (left - dx > mOrientationHelper.getStartAfterPadding()) {
                return left - mOrientationHelper.getStartAfterPadding();
            }
        }
        return dx;
    }

    private void fillChildren(RecyclerView.Recycler recycler) {
        for (int index = 0; index < mRectList.size(); index++) {
            View view = recycler.getViewForPosition(index);
            Rect rect = mRectList.get(index);
            if (rect.right <= mOrientationHelper.getStartAfterPadding() - mBaseOffset
                    || rect.left >= mOrientationHelper.getEndAfterPadding()) {
                removeAndRecycleView(view, recycler);
                continue;
            }
            addView(view);
            layoutDecorated(view, rect.left, rect.top, rect.right, rect.bottom);
            view.setAlpha(calculateViewAlpha(rect.left, rect.right - rect.left));
        }
    }

    private void resetViewsAlpha() {
        int count = getChildCount();
        for (int index = 0; index < count; index++) {
            View view = getChildAt(index);
            int left = getDecoratedLeft(view);
            int width = getDecoratedMeasuredWidth(view);
            view.setAlpha(calculateViewAlpha(left, width));
        }
    }

    public int calculateHorizontalOffset(boolean isLeft) {
        View view = getFocusedChild();
        if (view == null) {
            return 0;
        }
        int pos = getPosition(view);
        int startEdge = getDecoratedLeft(view);
        int baseOffset = startEdge - mLeftEdgeOffset;
        int parentLeft = mOrientationHelper.getStartAfterPadding();
        if (isLeft) {
            baseOffset = calculateLeftOffset(baseOffset, parentLeft);
        } else {
            baseOffset = calculateRightOffset(baseOffset);
        }
        return baseOffset;
    }

    private int calculateRightOffset(int baseOffset) {
        View lastChild = getChildAt(getChildCount() - 1);
        int lastPos = getPosition(lastChild);
        if (lastPos == getItemCount() - 1) {
            int rightDist = getDecoratedRight(lastChild) - getWidth() + 50;
            if (rightDist > 0) {
                if (rightDist < baseOffset) {
                    return rightDist;
                }
            } else {
                return 0;
            }
        }
        return baseOffset;
    }

    private int calculateLeftOffset(int baseOffset, int parentLeft) {
        if (baseOffset >= 0) {
            return 0;
        }
        View firstChild = getChildAt(0);
        int firstPos = getPosition(firstChild);
        if (firstPos == 0) {
            int firstEdge = getDecoratedLeft(firstChild);
            int firstOffset = firstEdge - parentLeft;
            if (firstOffset > 0) {
                return 0;
            }
            if (firstEdge <= 0 && Math.abs(firstOffset) < Math.abs(baseOffset)) {
                return firstOffset;
            }
        }
        return baseOffset;
    }

    private float calculateViewAlpha(int left, int width) {
        int parentLeft = mOrientationHelper.getStartAfterPadding();
        if (left >= parentLeft) {
            return 1.0f;
        }
        float allDistance = width + mBaseOffset;
        float throughDistance = parentLeft - left;
        float alpha = 1.0f;
        if (throughDistance != 0) {
            alpha = 1.0f - throughDistance / allDistance;
        }
        return alpha;
    }

    public int getScrollOffsetForPosition(boolean left, int position) {
        View firstView = getChildAt(0);
        View lastView = getChildAt(getChildCount() - 1);
        if (firstView == null || lastView == null) {
            return 0;
        }
        int firstItemPos = getPosition(firstView);
        int lastItemPos = getPosition(lastView);

        int parentWidth = getWidth();
        int offset = 0;
        if (left) {
            if (position < firstItemPos) {
                int firstLeft = mOrientationHelper.getDecoratedStart(firstView);
                for (int index = position; index < firstItemPos; index++) {
                    offset += mItemWidth;
                }
                offset -= firstLeft;
                offset += mLeftEdgeOffset;
                offset = -offset;
            } else if (position <= lastItemPos) {
                View view = getChildAt(position - firstItemPos);
                int viewStart = mOrientationHelper.getDecoratedStart(view);
                if (viewStart < mLeftEdgeOffset) {
                    offset = viewStart - mLeftEdgeOffset;
                }
            }
        } else {
            if (position > lastItemPos) {
                for (int index = lastItemPos + 1; index <= position; index++) {
                    offset += mItemWidth;
                }
                int lastRight = mOrientationHelper.getDecoratedEnd(lastView);
                int del = lastRight - parentWidth + mLeftEdgeOffset;
                offset -= del;
            } else if (position >= firstItemPos) {
                View view = getChildAt(position - firstItemPos);
                int viewEnd = mOrientationHelper.getDecoratedEnd(view);
                if (viewEnd > parentWidth - mLeftEdgeOffset) {
                    offset = viewEnd - parentWidth + mLeftEdgeOffset;
                }
            }
        }
        return offset;

    }

}