package com.ez.adapters.adapter;

import android.app.Activity;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ez.adapters.R;
import com.ez.adapters.base.BaseViewHolder;
import com.ez.adapters.interfaceabstract.IItemClick;

import java.util.ArrayList;
import java.util.List;

/**
 * 又一个超级adapter可以添加其他adapter
 * 类似于bilibili的首页（图片,文字,视频,商品列表,轮播图等多项混排显示）的样式，数据穿插无法区分的
 * <p>
 * 限制条件：
 * 1.bean必须继承{@link ISuperListBean}
 * 2.子adapter必须是{@link ISuperListAdapter},{@link BaseAdapterSl}的子类
 * 3.子adapter的type必须在{@link BaseSuperAdapter#mTypeMax}{@link BaseSuperAdapter#mTypeMin}之间
 * 4.GridLayoutManager必须提前设置或提前传入具体要求见{@link #BaseSuperListAdapter}
 * 其他限制暂时没发现
 */
public final class BaseSuperListAdapter<BEAN extends BaseSuperListAdapter.ISuperListBean> extends RecyclerView.Adapter<BaseViewHolder> {
    public final String TAG = getClass().getSimpleName();
    protected MyItemAdapter mItemAdapters = new MyItemAdapter();
    protected ISuperListDataObserver mObservers = new ISuperListDataObserver() {
        @Override
        public void notifyDataSetChanged() {
            BaseSuperListAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void notifyItemChanged(int position, ISuperListBean bean) {
            notifyItemChanged(position, 1, bean);
        }

        @Override
        public void notifyItemChanged(int positionStart, int itemCount, ISuperListBean bean) {
            int newPosition = getBaseAdapterPosition(bean, positionStart);
            BaseSuperListAdapter.this.notifyItemRangeChanged(newPosition, itemCount, null);
        }

        @Override
        public void notifyItemInserted(int position, ISuperListBean bean) {
            notifyItemInserted(position, 1, bean);
        }

        @Override
        public void notifyItemInserted(int positionStart, int itemCount, ISuperListBean bean) {
            int newPosition = getBaseAdapterPosition(bean, positionStart);
            BaseSuperListAdapter.this.notifyItemRangeInserted(newPosition, itemCount);
        }

        @Override
        public void notifyItemMoved(int fromPosition, int toPosition, ISuperListBean bean) {
            int newPosition = getBaseAdapterPosition(bean, fromPosition);
            BaseSuperListAdapter.this.notifyItemMoved(newPosition, newPosition + (toPosition - fromPosition));
        }

        @Override
        public void notifyItemRemoved(int position, ISuperListBean bean) {
            notifyItemRemoved(position, 1, bean);
        }

        @Override
        public void notifyItemRemoved(int positionStart, int itemCount, ISuperListBean bean) {
            int newPosition = getBaseAdapterPosition(bean, positionStart);
            BaseSuperListAdapter.this.notifyItemRangeRemoved(newPosition, itemCount);
        }
    };

    protected final Activity mActivity;
    protected final LayoutInflater mInflater;
    protected RecyclerView mRecyclerView;
    protected GridLayoutManager mLayoutManager;
    protected List<BEAN> mList;

    /**
     * 子adapter的信息,具体见{@link MyItemInfo#refreshItemInfo}
     */
    protected MyItemInfo mItemInfo = new MyItemInfo();

    /**
     * 注释同下
     */
    public BaseSuperListAdapter(Activity activity) {
        this(activity, null, new ArrayList<BEAN>());
    }

    /**
     * GridLayoutManager自动识别,但是setLayoutManager(glm)这个方法必须在addAdapter之前或在RecyclerView.setAdapter()之前.
     * 不是GridLayoutManager类型的可以忽略.
     * 例:错误的写法mRv.setAdapter(new BaseSuperListAdapter(mActivity).addAdapter(new XxxAdapter()));
     * mRv.setLayoutManager(new GridLayoutManager(mActivity, 2));
     * 正解:需要先setLayoutManager然后再写其他代码,原因见{@link #checkLayouManager}.
     */
    public BaseSuperListAdapter(Activity activity, @Nullable List<BEAN> list) {
        this(activity, null, list);
    }

    /**
     * @param manager 如果是GridLayoutManager需要用到setSpanSizeLookup这个方法
     */
    public BaseSuperListAdapter(Activity activity, GridLayoutManager manager, @Nullable List<BEAN> list) {
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);
        changedLayouManager(manager);
        mList = list;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mItemAdapters.getAdapter(viewType / BaseSuperAdapter.mTypeMinus).onCreateViewHolder(parent, viewType % BaseSuperAdapter.mTypeMinus, mInflater);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        mItemInfo.refreshItemInfo(position);
        //noinspection unchecked 未检查警告,此处忽略
        mItemInfo.mItemAdapter.bindViewHolder(holder, mItemInfo.mItemPosition, mList.get(mItemInfo.mListPosition));
    }

    @Override
    public int getItemViewType(int position) {
        mItemInfo.refreshItemInfo(position);
        //noinspection unchecked 未检查警告,此处忽略
        int itemType = mItemInfo.mItemAdapter.getItemViewType(mItemInfo.mItemPosition, mList.get(mItemInfo.mListPosition));
        if (itemType < BaseSuperAdapter.mTypeMin || itemType >= BaseSuperAdapter.mTypeMax)
            throw new RuntimeException("你的type必须在" + BaseSuperAdapter.mTypeMin + "~" + BaseSuperAdapter.mTypeMax + "之间");
        //根据mItemAdapters的position返回type，取的时候比较方便
        return mItemAdapters.getPosition(mItemInfo.mItemAdapter.getClass()) * BaseSuperAdapter.mTypeMinus + itemType;
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mList != null) {
            for (BEAN bean : mList) {
                //noinspection unchecked,ConstantConditions 未检查警告,空指针警告,此处忽略
                count += mItemAdapters.getAdapter(bean.getItemAdapterClass()).getItemCount(bean);
            }
        }
        return count;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        checkLayouManager();
    }

    protected void checkLayouManager() {
        if (mRecyclerView == null) return;
        RecyclerView.LayoutManager mamager = mRecyclerView.getLayoutManager();
        if ((mLayoutManager == null || mLayoutManager != mamager) && mamager instanceof GridLayoutManager) {
            changedLayouManager((GridLayoutManager) mamager);
        }
    }

    /**
     * 根据bean对象和adapter的相对位置获取绝对位置
     */
    protected int getBaseAdapterPosition(ISuperListBean bean, int itemAdapterPosition) {
        int position = itemAdapterPosition;
        for (BEAN listBean : mList) {
            if (listBean == bean) {
                return position;
            } else {
                //noinspection unchecked,ConstantConditions 未检查警告,空指针警告,此处忽略
                position += mItemAdapters.getAdapter(bean.getItemAdapterClass()).getItemCount(listBean);
            }
        }
        throw new RuntimeException("在list中没有找到传入的bean对象");
    }

    protected class MyItemInfo {
        /**
         * 使用之前请调用{@link #refreshItemInfo}
         * list的position,子adapter的所需要的相对position
         */
        protected int mListPosition, mItemPosition;
        protected ISuperListAdapter mItemAdapter;

        /**
         * 根据超级adapter的position返回子adapter的信息
         */
        protected MyItemInfo refreshItemInfo(int position) {
            //itemdapter的position=0时的真实位置
            int itemStartPosition = 0;
            for (int i = 0; i < mList.size(); i++) {
                BEAN bean = mList.get(i);
                ISuperListAdapter adapter = mItemAdapters.getAdapter(bean.getItemAdapterClass());
                //noinspection unchecked,ConstantConditions 未检查警告,空指针警告,此处忽略
                int itemCount = adapter.getItemCount(bean);
                int nextStartPosition = itemStartPosition + itemCount;
                //下一个adapter的位置比position大说明当前type就在这个adapter中
                if (nextStartPosition > position) {
                    mListPosition = i;
                    mItemAdapter = adapter;
                    mItemPosition = position - itemStartPosition;
                    return this;
                } else {
                    //循环相加
                    itemStartPosition = nextStartPosition;
                }
            }
            throw new RuntimeException("没有取到对应的type,可能你没有(及时)刷新adapter");
        }
    }

    /**
     * position,adapter,class唯一并且可以互相取值
     */
    protected class MyItemAdapter {
        protected SimpleArrayMap<Class<? extends ISuperListAdapter>, Integer> mMap = new SimpleArrayMap<>(8);
        protected ArrayList<ISuperListAdapter> mList = new ArrayList<>(8);

        protected void addAdapter(ISuperListAdapter... adapters) {
            for (ISuperListAdapter adapter : adapters) {
                adapter.registerDataSetObserver(mObservers);
                if (!mMap.containsKey(adapter.getClass())) {
                    mList.add(adapter);
                    mMap.put(adapter.getClass(), mList.size() - 1);
                }
            }
        }

        protected void addAdapter(List<? extends ISuperListAdapter> adapters) {
            for (ISuperListAdapter adapter : adapters) {
                adapter.registerDataSetObserver(mObservers);
                if (!mMap.containsKey(adapter.getClass())) {
                    mList.add(adapter);
                    mMap.put(adapter.getClass(), mList.size() - 1);
                }
            }
        }

        protected ISuperListAdapter getAdapter(int position) {
            return mList.get(position);
        }

        protected ISuperListAdapter getAdapter(Class<? extends ISuperListAdapter> cls) {
            Integer index = mMap.get(cls);
            if (index == null) return null;
            return mList.get(index);
        }

        protected int getPosition(Class<? extends ISuperListAdapter> cls) {
            return mMap.get(cls);
        }

        protected void remove(int position) {
            ISuperListAdapter remove = mList.remove(position);
            mMap.remove(remove.getClass());
        }

        protected void remove(Class<? extends ISuperListAdapter> cls) {
            Integer remove = mMap.remove(cls);
            if (remove != null) {
                mList.remove((int) remove);
            }
        }

        protected void remove(ISuperListAdapter adapter) {
            Integer remove = mMap.remove(adapter.getClass());
            if (remove != null) {
                mList.remove((int) remove);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 以下是增加的方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 添加adapter.重复则不会被添加,必须先删除
     */
    public BaseSuperListAdapter addAdapter(@NonNull ISuperListAdapter... adapters) {
        mItemAdapters.addAdapter(adapters);
        checkLayouManager();
        notifyDataSetChanged();
        return this;
    }

    public BaseSuperListAdapter addAdapter(@NonNull List<? extends ISuperListAdapter> adapters) {
        mItemAdapters.addAdapter(adapters);
        checkLayouManager();
        notifyDataSetChanged();
        return this;
    }

    /**
     * 设置新的list数据并刷新adapter
     */
    public void setListAndNotifyDataSetChanged(List<BEAN> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public List<BEAN> getList() {
        return mList;
    }

    /**
     * 删除指定adapter
     */
    @Deprecated
    public BaseSuperListAdapter removeAdapter(int adapterPosition) {
        mItemAdapters.remove(adapterPosition);
        return this;
    }

    @Deprecated
    public BaseSuperListAdapter removeAdapter(Class<? extends ISuperListAdapter> adapterClass) {
        mItemAdapters.remove(adapterClass);
        return this;
    }

    /**
     * 把rv的LayoutManager改成其他的GridLayoutManager时.此方法理论上没啥用
     */
    public void changedLayouManager(GridLayoutManager manager) {
        if (manager == null) return;
        mLayoutManager = manager;
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                mItemInfo.refreshItemInfo(position);
                //noinspection unchecked 未检查警告,此处忽略
                return mItemInfo.mItemAdapter.getSpanSize(mItemInfo.mItemPosition, mList.get(mItemInfo.mListPosition));
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 供外部使用的接口和实现类
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * observer
     */
    public interface ISuperListDataObserver {

        /**
         * 刷新全部的adapter数据,其他方法均是局部刷新
         */
        void notifyDataSetChanged();

        /**
         * @param bean list的bean数据,没有bean的话无法确定位置
         */
        void notifyItemChanged(int position, ISuperListBean bean);

        void notifyItemChanged(int positionStart, int itemCount, ISuperListBean bean);

        void notifyItemInserted(int position, ISuperListBean bean);

        void notifyItemInserted(int positionStart, int itemCount, ISuperListBean bean);

        void notifyItemMoved(int fromPosition, int toPosition, ISuperListBean bean);

        void notifyItemRemoved(int position, ISuperListBean bean);

        void notifyItemRemoved(int positionStart, int itemCount, ISuperListBean bean);
    }

    /**
     * adapter接口,见实现类{@link BaseAdapterSl}
     */
    public interface ISuperListAdapter<VH extends BaseViewHolder, BEAN extends ISuperListBean> extends BaseSuperListAdapter.ISuperListDataObserver {
        /**
         * observe主要用于notify
         */
        void registerDataSetObserver(@NonNull BaseSuperListAdapter.ISuperListDataObserver observer);

        void unregisterDataSetObserver(@NonNull BaseSuperListAdapter.ISuperListDataObserver observer);

        int getItemCount(@NonNull BEAN bean);

        /**
         * @param viewType 相对该adapter的type
         */
        @NonNull
        VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType, LayoutInflater inflater);

        /**
         * @param position 该adapter内部的position，{@link #getItemCount}
         * @param bean     整个adapter所对应的bean，不是position所对应的
         */
        void bindViewHolder(@NonNull VH holder, int position, @NonNull BEAN bean);

        int getSpanSize(int position, @NonNull BEAN bean);

        /**
         * @return 不能超出范围, 超出就会被当成其他adapter的type(如果不够使用可以自行修改{@link BaseSuperAdapter#mTypeMax},{@link BaseSuperAdapter#mTypeMin}的值)
         */
        @IntRange(from = BaseSuperAdapter.mTypeMin, to = BaseSuperAdapter.mTypeMax)
        int getItemViewType(int position, @NonNull BEAN bean);

        void setOnItemClickListener(IItemClick listener);
    }

    /**
     * 对{@link ISuperListAdapter}的抽象实现
     */
    public static abstract class BaseAdapterSl<VH extends BaseViewHolder, BEAN extends ISuperListBean> implements ISuperListAdapter<VH, BEAN> {

        protected ArraySet<ISuperListDataObserver> mObservers = new ArraySet<>();

        protected IItemClick mListener;
        protected final Activity mActivity;
        protected final LayoutInflater mInflater;

        public BaseAdapterSl(Activity activity) {
            mActivity = activity;
            mInflater = LayoutInflater.from(mActivity);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // 继承下来的基本实现,正常情况不需要再重写
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public void registerDataSetObserver(@NonNull BaseSuperListAdapter.ISuperListDataObserver observer) {
            mObservers.add(observer);
        }

        @Override
        public void unregisterDataSetObserver(@NonNull BaseSuperListAdapter.ISuperListDataObserver observer) {
            mObservers.remove(observer);
        }

        @Override
        public final void bindViewHolder(@NonNull VH holder, int position, @NonNull BEAN bean) {
            holder.itemView.setTag(R.id.tag_view_click, position);
            holder.itemView.setOnClickListener(mListener);
            holder.itemView.setOnLongClickListener(mListener);
            onBindViewHolder(holder, position, bean);
        }

        @Override
        public void setOnItemClickListener(IItemClick listener) {
            mListener = listener;
            notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetChanged() {
            for (BaseSuperListAdapter.ISuperListDataObserver observer : mObservers) {
                observer.notifyDataSetChanged();
            }
        }

        @Override
        public void notifyItemChanged(int position, ISuperListBean bean) {
            notifyItemChanged(position, 1, bean);
        }

        @Override
        public void notifyItemChanged(int positionStart, int itemCount, ISuperListBean bean) {
            for (BaseSuperListAdapter.ISuperListDataObserver observer : mObservers) {
                observer.notifyItemChanged(positionStart, itemCount, bean);
            }
        }

        @Override
        public void notifyItemInserted(int position, ISuperListBean bean) {
            notifyItemInserted(position, 1, bean);
        }

        @Override
        public void notifyItemInserted(int positionStart, int itemCount, ISuperListBean bean) {
            for (BaseSuperListAdapter.ISuperListDataObserver observer : mObservers) {
                observer.notifyItemInserted(positionStart, itemCount, bean);
            }
        }

        @Override
        public void notifyItemMoved(int fromPosition, int toPosition, ISuperListBean bean) {
            for (BaseSuperListAdapter.ISuperListDataObserver observer : mObservers) {
                observer.notifyItemMoved(fromPosition, toPosition, bean);
            }
        }

        @Override
        public void notifyItemRemoved(int position, ISuperListBean bean) {
            notifyItemRemoved(position, 1, bean);
        }

        @Override
        public void notifyItemRemoved(int positionStart, int itemCount, ISuperListBean bean) {
            for (BaseSuperListAdapter.ISuperListDataObserver observer : mObservers) {
                observer.notifyItemRemoved(positionStart, itemCount, bean);
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // 以下是经常需要重写的方法
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public int getItemCount(@NonNull BEAN bean) {
            return 1;
        }

        @Override
        public int getSpanSize(int position, @NonNull BEAN bean) {
            return 1;
        }

        @Override
        public int getItemViewType(int position, @NonNull BEAN bean) {
            return 0;
        }

        /**
         * @param position 该adapter内部的position，{@link #getItemCount}
         * @param bean     整个adapter所对应的bean，不是position所对应的
         */
        protected abstract void onBindViewHolder(@NonNull VH holder, int position, @NonNull BEAN bean);

        //还有个onCreateViewHolder就不写了
    }

    /**
     * 你的最外层bean必须继承该接口
     */
    public interface ISuperListBean {
        Class<? extends ISuperListAdapter> getItemAdapterClass();
    }
}