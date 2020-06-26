# 提供一些基础工具类，在代码中直接类.方法即可实用
## 举个栗子
```
    // 方法类集合
    //BDate 日期工具类  BArray 数组工具类   BSql sqlite 工具类  BString 字符串操作类
    //BObject 元素操作  BFile 文件IO操作
    //例子
    Bdebug.trace("hello");
```

# Socket封装的食用
## 快速的搞定套接字连接
```
    /**
     * 初始化Socket
     */
    private void initSocket() {
        socketClient = new SocketClient();
        socketClient.createConnection("49.234.186.237",9123,true);
        socketClient.setSocketCallback(this).start();
    }
    //提供如下的监听回调类 :SocketCallback
    public interface SocketCallback {
        //出错
        void onError(Exception e);
        //关闭
        void onClosed(Socket socket);
        //已连接
        void onConnected(Socket socket);
        //连接失败
        void onConnectFail(Socket socket, boolean needReconnect);
        //接收
        void onReceive(Socket socket, ByteArrayOutputStream byteArray);
    }


```


# BaseAdapter简单食用

## 详细示例见本项目app下的MainActivity
一个listAdapter只需要如下几行
```
    public class ListAdapter extends BaseAdapterRvList<BaseViewHolder, String> {
        public ListAdapter(@NonNull Activity activity, List<String> list) {
            super(activity, list);
        }
        @Override
        protected void onBindVH(BaseViewHolder holder, int listPosition, String s) {
            //当然，你也可以继承BaseViewHolder自己用黄油刀生成
            holder.setText(R.id.text, s).setViewVisible(R.id.text, s == null ? View.GONE : View.VISIBLE);
        }
        @NonNull
        @Override
        protected BaseViewHolder onCreateVH(ViewGroup parent, LayoutInflater inflater) {
            return new BaseViewHolder(parent,R.layout.adapter);
        }
    }
```
自带点击事件
```
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            protected void onItemClick(View view, int listPosition) {
            }

            @Override
            protected boolean onItemLongClick(View view, int listPosition) {
                return true;
            }

            @Override
            protected void onFooterClick(View view) {
                super.onFooterClick(view);
            }
            ...Header、LongClick等
        });
```
自带header、footer
```
        adapter.addHeaderView(view);
        adapter.addFooterView(view);
```
懒人专属
```
BaseAdapterRvList<BaseViewHolder, String> adapter = BaseAdapterRvList.createAdapter(R.layout.adapter, new OnAdapterBindListener<String>() {
    @Override
    public void onBindVH(BaseViewHolder holder, int listPosition, String s) {
        holder.setText(R.id.text, s);
    }
});
mRv.setAdapter(adapter);
//...刷新数据时
adapter.setListAndNotifyDataSetChanged(list);
```
ViewPager的Fragment更简单
```
mVp.setAdapter(new BaseAdapterVpFrag(getSupportFragmentManager(), mFrags));
//或
mVp.setAdapter(new BaseAdapterVpFrag(getSupportFragmentManager(), frag1,frag2...));
//动态修改frag
        mAdapter = new BaseAdapterVpStateFrag(getSupportFragmentManager(), mFrags);
        mVp.setAdapter(mAdapter);
        ...
        mAdapter.getFragments().add(xxx);//由于内部有新的list，所以并不能用自己的mFrags
        mAdapter.getFragments().remove(yyy);
        mAdapter.notifyDataSetChanged();
//解决动态修改刷新白屏的问题
        FragmentNotifyAdapter adapter = new FragmentNotifyAdapter(getSupportFragmentManager(), mFrags);
        mVp.setAdapter(adapter);
        ...
        adapter.notifyAllItem(1);//保留展示在界面上的那个这样就不会白屏了，想要刷新保留的frag当然需要自己实现了，详见app下的示例
```

## 导入方式
你的build.gradle要有jitpack.io，大致如下
```
allprojects {
    repositories {
        maven { url 'http://maven.aliyun.com/nexus/content/groups/public/' }
        maven { url 'https://jitpack.io' }
        google()
        jcenter()
    }
}
```
然后导入
`implementation（或api） 'com.github.irunmyway:AndroidBaseUtil:1.0.0.2'`

AndroidX：
`implementation（或api） 'com.github.irunmyway:AndroidBaseUtil:2.0.0.5'`
