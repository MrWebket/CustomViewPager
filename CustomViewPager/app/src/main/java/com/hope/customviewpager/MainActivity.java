package com.hope.customviewpager;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by Hope on 15/11/4.
 */
public class MainActivity extends Activity{

    private CustomViewflipper mCentontFlipper;

    private List<String> mDataSource = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCentontFlipper = (CustomViewflipper) findViewById(R.id.custom_gallery);

        for (int i = 0; i < 5; i++) {
            mDataSource.add("test + " + i);
        }

        initCentontFlipper(mCentontFlipper);

        mCentontFlipper.setPageChangeListener(new CustomViewflipper.OnPageChangeListener() {
            @Override
            public void onPageChange(View view, int mCurrentItem) {
                ViewHolder holder = (ViewHolder) view.getTag();

                holder.btn.setText(mDataSource.get(mCurrentItem));
            }

            @Override
            public void onChangePosition(int item) {

            }
        });

        //设置数据源
        mCentontFlipper.setDataSource(mDataSource);

        //支持无限滑动
        mCentontFlipper.setAllowCycle(true);


    }

    private void initCentontFlipper(CustomViewflipper viewflipper) {
        viewflipper.addFilpperChildView(createChildView(), createChildView());
    }

    private View createChildView() {
       View view =  getLayoutInflater().inflate(R.layout.main_item, null);

        ViewHolder holder = new ViewHolder();
        holder.btn = (Button)view.findViewById(R.id.btn);
        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "测试哇", Toast.LENGTH_SHORT).show();
            }
        });
        view.setTag(holder);
        return view;
    }

    private class ViewHolder {
        private Button btn;
    }
}
