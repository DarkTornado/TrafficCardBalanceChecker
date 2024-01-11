package com.darktornado.cardbalancechecker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);

        TextView txt = new TextView(this);
        txt.setText("스마트폰의 NFC 기능을 활성화시키고, 스마트폰 뒷면에 교통카드를 태그해주세요.\n\n" +
                "[사용 가능 카드 목록]\n" +
                " - 한국 : 티머니, 캐시비, 레일플러스\n" +
                " - 일본 : Suica 및 상호호환카드");
        layout.addView(txt);

        int pad = dip2px(16);
        layout.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }



    private void toast(final String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dip2px(int dips) {
        return (int) Math.ceil(dips * getResources().getDisplayMetrics().density);
    }
}