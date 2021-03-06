package me.iacn.mbestyle.presenter;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.iacn.mbestyle.R;
import me.iacn.mbestyle.bean.RequestBean;
import me.iacn.mbestyle.bean.leancloud.LeanQueryBean;
import me.iacn.mbestyle.network.LeanApi;
import me.iacn.mbestyle.ui.fragment.RequestFragment;
import me.iacn.mbestyle.util.PackageUtils;

/**
 * Created by iAcn on 2017/2/19
 * Email i@iacn.me
 */

public class RequestPresenter {

    private RequestFragment mView;
    private int mLoadingCount;

    public RequestPresenter(RequestFragment mView) {
        this.mView = mView;
    }

    public void loadInstallApp() {
        Flowable.create(new FlowableOnSubscribe<List<RequestBean>>() {
            @Override
            public void subscribe(FlowableEmitter<List<RequestBean>> e) throws Exception {
                // 获得已适配的所有主 Activity 全名
                XmlResourceParser xml = mView.getResources().getXml(R.xml.appfilter);
                Set<String> adaptedActivitySet = new HashSet<>();

                while (xml.getEventType() != XmlResourceParser.END_DOCUMENT) {
                    if (xml.getEventType() == XmlPullParser.START_TAG) {
                        if (xml.getName().startsWith("item")) {
                            String component = xml.getAttributeValue(null, "component");
                            if (!component.startsWith(":")) {
                                // 跳过 Component 系统图标模糊匹配
                                adaptedActivitySet.add(findActivityName(component));
                            }
                        }
                    }
                    xml.next();
                }

                PackageManager manager = mView.getActivity().getPackageManager();
                List<ResolveInfo> list = PackageUtils.getAppByMainIntent(mView.getActivity());
                List<RequestBean> apps = new ArrayList<>();

                StringBuilder builder = new StringBuilder();

                for (ResolveInfo info : list) {
                    // 排除已经适配的应用
                    if (adaptedActivitySet.contains(info.activityInfo.name)) continue;

                    RequestBean bean = new RequestBean();
                    bean.name = info.loadLabel(manager).toString();
                    bean.icon = info.loadIcon(manager);
                    bean.packageName = info.activityInfo.packageName;

                    bean.activity = builder.append(info.activityInfo.packageName)
                            .append("/")
                            .append(info.activityInfo.name)
                            .toString();

                    builder.delete(0, builder.length());

                    apps.add(bean);
                }

                e.onNext(apps);
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<RequestBean>>() {
                    @Override
                    public void accept(@NonNull List<RequestBean> list) throws Exception {
                        mView.onLoadData(list);
                    }
                });
    }

    public void getRequestTotal(String packageName, final RequestBean bean, final TextView textView) {
        if (bean.total != -1) {
            // 已经加载过一次，直接设置
            setRequestTotal(textView, bean.total);
            return;
        }

        // 加载数自加1
        mLoadingCount++;

        LeanApi.getInstance()
                .queryRequestTotal(packageName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<LeanQueryBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(LeanQueryBean leanBean) {
                        if (leanBean.results == null || leanBean.results.size() == 0) {
                            setRequestTotal(textView, 0);
                            bean.total = 0;
                        } else {
                            LeanQueryBean.ResultsBean lean = leanBean.results.get(0);
                            setRequestTotal(textView, lean.requestTotal);

                            bean.total = lean.requestTotal;
                            bean.objectId = lean.objectId;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();

                        // 发生异常直接中断，所以手动调用一下
                        handleLoadingState();
                    }

                    @Override
                    public void onComplete() {
                        handleLoadingState();
                    }
                });
    }

    private void handleLoadingState() {
        mLoadingCount--;

        if (mLoadingCount == 0) {
            // 最后1个 Item 已加载完成
            mView.stopLoadingState();
        }
    }

    private String findActivityName(String component) {
        try {
            String str = component.split("/")[1];
            return str.substring(0, str.length() - 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setRequestTotal(TextView textView, int value) {
        if (value == 0) {
            textView.setText(R.string.not_be_requested);
        } else {
            textView.setText(String.format(Locale.getDefault(), mView.getString(R.string.request_sum), value));
        }
    }
}