package com.hope.customviewpager;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

public class CustomViewflipper extends FrameLayout {
	/**
	 * 阈值
	 */
	private static final int THRESHOLD_VALUE = 30;

	public final static int SCROLL_DURATION = 300; // scroll back duration

	private static final int SNAP_VELOCITY_DIP_PER_SECOND = 300;

	/**
	 * 是否拦截滑动状态--拦截
	 */
	private static final int SCROLL_STATE_FORBID = 1;
	/**
	 * 是否拦截滑动状态--不拦截
	 */
	private static final int SCROLL_STATE_NORMAL = 2;

	private static final int STATE_MOVE_RIGHT = 1;
	private static final int STATE_MOVE_LEFT = 2;

	private static final int LAYOUT_STATE_REFRESH = 1;
	private static final int LAYOUT_SATE_MOVE = 2;
	private static final int LAYOUT_STATE_NORMAL = 3;

	private int mScrollState = SCROLL_STATE_NORMAL;

	private int mLayoutState = LAYOUT_STATE_NORMAL;

	private int mMoveState;

	private int mCurrentItem;

	private List<Object> mDataSource = new ArrayList<Object>();

	private OnPageChangeListener mOnPageChangeListener;

	/** 速度跟踪 */
	private VelocityTracker mVelocityTracker;

	private int mMaximumVelocity;

	private int mDensityAdjustedSnapVelocity;

	private int mChildWidth = 0;
	private int mChildHeight = 0;

	/**
	 * 是否滚动至下一个Page
	 */
	private boolean isScrollNext = false;

	private int mWhichChild;

	private Scroller mScroller;

    /**
     * 是否允许无限轮训
     */
    private boolean isAllowCycle = false;

	public CustomViewflipper(Context paramContext) {
		super(paramContext);

		init();
	}

	public CustomViewflipper(Context paramContext, AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);

		init();
	}

	private void init() {
		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
		mDensityAdjustedSnapVelocity = (int) (displayMetrics.density * SNAP_VELOCITY_DIP_PER_SECOND);

		// 视图树观察者,用于获取ViewFlipper的宽度
		ViewTreeObserver vto = getViewTreeObserver();
		// 当一个视图树将要绘制时，所要调用的回调函数的接口类
		vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				mChildWidth = getCurrentView().getMeasuredWidth();
				mChildHeight = getCurrentView().getMeasuredHeight();
				return true;
			}
		});

		mScroller = new Scroller(getContext(), new DecelerateInterpolator());
	}

	public View getCurrentView() {
		return this.getChildAt(getDisplayedChild());
	}


    /**
     * 是否支持无限滑动
     * @param isAllowCycle
     */
    public void setAllowCycle(boolean isAllowCycle) {
        this.isAllowCycle = isAllowCycle;
    }

	public void addFilpperChildView(View... childView) {
		if (getChildCount() != 0) {
			removeAllViews();
		}

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		addView(childView[0], params);
		addView(childView[1], params);

		mWhichChild = getChildCount() - 1;
		requestLayout();
	}

	public int getCurrentItem() {
		return mCurrentItem;
	}

	public void setDataSource(List<? extends Object> list) {
		mDataSource.clear();

		if (list != null) {
			mDataSource.addAll(list);
		}

		if ((mDataSource == null || mDataSource.size() == 1)) {
			mScrollState = SCROLL_STATE_FORBID;
		} else {
			mScrollState = SCROLL_STATE_NORMAL;
		}

		mCurrentItem = 0;

		displayCurrentView();
	}

	private void displayMoveView(boolean isLeft) {
		if (mDataSource != null && mDataSource.size() > 0) {
			int index = isLeft ? mCurrentItem + 1 > mDataSource.size() - 1 ? 0 : mCurrentItem + 1 : mCurrentItem - 1 < 0 ? mDataSource.size() - 1 : mCurrentItem - 1;
			display(getNextView(), index);
		}
	}

	private void display(View view, int mCurrentItem) {
		if(mOnPageChangeListener != null) {
			mOnPageChangeListener.onPageChange(view, mCurrentItem);
		}
	}

	public void setSelect(int mCurrentItem) {
		this.mCurrentItem = mCurrentItem;

		resetState();

		displayCurrentView();
	}

	private void displayCurrentView() {
		if (mDataSource != null && mDataSource.size() > 0) {
			display(getCurrentView(), mCurrentItem);
		}
	}

	private int getDisplayedChild() {
		return mWhichChild;
	}

	/**
	 * 下一个视图
	 */
	private void setNextViewItem() {
		mCurrentItem = mCurrentItem + 1 > mDataSource.size() - 1 ? 0 : ++mCurrentItem;

		if (mOnPageChangeListener != null) {
			mOnPageChangeListener.onChangePosition(mCurrentItem);
		}
	}

	/**
	 * 上一个视图
	 */
	private void setPrevViewItem() {
		mCurrentItem = mCurrentItem - 1 < 0 ? mDataSource.size() - 1 : --mCurrentItem;//

		if (mOnPageChangeListener != null) {
			mOnPageChangeListener.onChangePosition(mCurrentItem);
		}
	}

	private boolean isDisplayNext = false;
	private boolean isDisplayPrevious = false;

	private boolean isInterception = false;

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		if (mChildWidth == 0) {
			mChildWidth = getCurrentView().getWidth();
			mChildHeight = getCurrentView().getHeight();
		}

		switch (mLayoutState) {
		case LAYOUT_STATE_NORMAL:
		case LAYOUT_STATE_REFRESH:
			getCurrentView().layout(0, 0, mChildWidth, mChildHeight);
			getNextView().layout(mChildWidth, 0, mChildWidth + mChildWidth, mChildHeight);
			break;
		case LAYOUT_SATE_MOVE:
			getCurrentView().layout(deltaX, 0, deltaX + mChildWidth, mChildHeight);

			switch (mMoveState) {
			case STATE_MOVE_RIGHT: // 往右边滑动
				int next = -(mChildWidth - deltaX);
				showPreviousView();
				getNextView().layout(next, 0, next + mChildWidth, mChildHeight);
				break;
			case STATE_MOVE_LEFT: // 往左边滑动
				showNextView();

				int xLast = mChildWidth - Math.abs(deltaX);
				getNextView().layout(xLast, 0, xLast + mChildWidth, mChildHeight);
				break;
			default:
				break;
			}
			break;

		default:
			break;
		}
	}

	private void showPreviousView() {
		if (!isDisplayPrevious) {
			displayMoveView(false);
			isDisplayNext = false;

			isDisplayPrevious = true;
		}
	}

	private void showNextView() {
		if (!isDisplayNext) {
			displayMoveView(true);
			isDisplayPrevious = false;

			isDisplayNext = true;
		}
	}

	private View getNextView() {
		int mNextChild = getDisplayedChild() + 1;
		if (mNextChild >= getChildCount()) {
			mNextChild = 0;
		}
		return getChildAt(mNextChild);
	}

	private float mDownX;
	private float mDownY;


	private int startX = 0;
	private int deltaX;


	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		int x = (int)ev.getX();
		int y = (int)ev.getY();
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDownX = ev.getX();
				mDownY = ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				int lastY = (int) ev.getY();
				int lastX = (int) ev.getX();
				if (Math.abs(lastX - mDownX) > Math.abs(lastY - mDownY)) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
			default:
				break;
		}
		return super.dispatchTouchEvent(ev);
	}


	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!mScroller.isFinished()) {
			return true;
		}
		if (mVelocityTracker == null)
			mVelocityTracker = VelocityTracker.obtain();

		mVelocityTracker.addMovement(ev);

		int action = ev.getAction();

		int x = (int) ev.getRawX();
		int y = (int) ev.getRawY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:

			startX = x;
			mDownX = ev.getX();
			mDownY = ev.getY();
			resetState();
			break;
		case MotionEvent.ACTION_MOVE:
			if(!isAllowCycle) {
                if((mCurrentItem == 0 && x > startX) || (mCurrentItem == mDataSource.size() - 1 && x < startX)) {
					isInterception = true;

					return true;
                }
            }
			isInterception = false;
			if (mScrollState == SCROLL_STATE_NORMAL && mScroller.isFinished()) {
				deltaX = x - startX;

				mMoveState = deltaX > 0 ? STATE_MOVE_RIGHT : STATE_MOVE_LEFT;

				if (deltaX != 0) {
					mLayoutState = LAYOUT_SATE_MOVE;
					requestLayout();
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL: // Cancel 不能让上级相应
		case MotionEvent.ACTION_UP:

			if(!isInterception) {
				int cancelX = x;
				int mdeltaX = Math.abs(cancelX - startX);
				switch (mScrollState) {
					case SCROLL_STATE_NORMAL:
						mLayoutState = LAYOUT_STATE_NORMAL;

						final VelocityTracker velocityTracker = mVelocityTracker;

						// 计算当前速度
						velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

						// x方向的速度
						int velocityX = (int) velocityTracker.getXVelocity();

						if ((velocityX > mDensityAdjustedSnapVelocity || cancelX - startX < -mChildWidth / 2)) { // 往左


							mScroller.startScroll(0, y, mChildWidth - Math.abs(deltaX), 0, SCROLL_DURATION);
							isScrollNext = true;
							return true;
						} else if ((velocityX < -mDensityAdjustedSnapVelocity || cancelX - startX > mChildWidth / 2)) {
							mScroller.startScroll(0, y, mChildWidth - Math.abs(deltaX), 0, SCROLL_DURATION);

							isScrollNext = true;

							return true;
						} else {
							isScrollNext = false;

							mScroller.startScroll(0, y, deltaX, 0, SCROLL_DURATION);
							if (Math.abs(mdeltaX) < THRESHOLD_VALUE) {

								return super.onInterceptTouchEvent(ev);
							} else {
								return true;
							}
						}
					case SCROLL_STATE_FORBID:
						if (Math.abs(mdeltaX) < THRESHOLD_VALUE) {
							return super.onInterceptTouchEvent(ev);
						} else {
							return true;
						}

					default:
						break;
				}
			}

		default:
			break;
		}

		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return true;

	};

	private void resetState() {
		mLayoutState = LAYOUT_STATE_NORMAL;

		deltaX = 0;

		isDisplayNext = false;
		isDisplayPrevious = false;

		requestLayout();
	}

	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			switch (mMoveState) {
			case STATE_MOVE_RIGHT: // 往右边滑动
				int next = -(mChildWidth - deltaX);

				showPreviousView();

				if (isScrollNext) {
					getNextView().layout(next + mScroller.getCurrX(), 0, next + mChildWidth + mScroller.getCurrX(), mChildHeight);
					getCurrentView().layout(deltaX + mScroller.getCurrX(), 0, mScroller.getCurrX() + deltaX + mChildWidth, mChildHeight);

					if (mScroller.isFinished()) {
						setPrevViewItem();

//						mWhichChild = mWhichChild == 0 ? 1 : 0;

						display(getCurrentView(), getCurrentItem());

						resetState();
					}
				} else {
					getCurrentView().layout(deltaX - mScroller.getCurrX(), 0, deltaX - mScroller.getCurrX() + mChildWidth, mChildHeight);
					getNextView().layout(next - mScroller.getCurrX(), 0, next + mChildWidth - mScroller.getCurrX(), mChildHeight);

					if (mScroller.isFinished()) {
						resetState();
					}
				}
				break;
			case STATE_MOVE_LEFT: // 往左边滑动
				showNextView();

				if (isScrollNext) {
					int xLast = mChildWidth - Math.abs(deltaX);
					getNextView().layout(xLast - mScroller.getCurrX(), 0, xLast + mChildWidth - mScroller.getCurrX(), mChildHeight);
					getCurrentView().layout(deltaX - mScroller.getCurrX(), 0, deltaX + mChildWidth - mScroller.getCurrX(), mChildHeight);
					
					if (mScroller.isFinished()) {
						setNextViewItem();
//						mWhichChild = mWhichChild == 0 ? 1 : 0;

						display(getCurrentView(), getCurrentItem());

						resetState();
					}
				} else {
					getCurrentView().layout(deltaX - mScroller.getCurrX(), 0, deltaX - mScroller.getCurrX() + mChildWidth, mChildHeight);
					int xLast = mChildWidth - Math.abs(deltaX);
					getNextView().layout(xLast - mScroller.getCurrX(), 0, xLast + mChildWidth - mScroller.getCurrX(), mChildHeight);

					if (mScroller.isFinished()) {
						resetState();
					}
				}
				break;
			default:
				break;
			}

		}
		postInvalidate();
	}

	public void setPageChangeListener(OnPageChangeListener listener) {
		this.mOnPageChangeListener = listener;
	}

	public interface OnPageChangeListener {
		public void onPageChange(View view, int mCurrentItem);

		public void onChangePosition(int item);
	}
}
