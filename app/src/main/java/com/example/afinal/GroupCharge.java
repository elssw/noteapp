package com.example.afinal;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afinal.model.Category;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class GroupCharge extends AppCompatActivity {
    private static final int MAX_LEN = 20;
    private TextView tvAmountDisplay;
    private TextView tvNote;
    private StringBuilder amountBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_charge);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        tvAmountDisplay.setText("NT$0");

        //備註
        tvNote = findViewById(R.id.note);
        tvNote.setOnClickListener(v -> {
            //置中
            TextView customTitle = new TextView(GroupCharge.this);
            customTitle.setText("備註");
            customTitle.setGravity(Gravity.CENTER);
            customTitle.setTextSize(20);


            EditText input = new EditText(GroupCharge.this);
            input.setHint("請輸入備註");


            AlertDialog dialog = new AlertDialog.Builder(GroupCharge.this)
                    .setCustomTitle(customTitle)
                    .setView(input)
                    .setPositiveButton("確定", (d, w) -> {
                        String text = input.getText().toString().trim();
                        if (!text.isEmpty()) {
                            tvNote.setText(text);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .create();

            dialog.show();
        });

        //類別 RecyclerView
        RecyclerView rv = findViewById(R.id.items);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        List<Category> cats = Arrays.asList(
                new Category(R.drawable.ic_group,      "蔡芯惠"),
                new Category(R.drawable.ic_group,    "群組"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_setting,  "設定")
        );
        rv.setAdapter(new CategoryAdapter(this, cats));

        //數字、運算符
        int[] inputIds = {
                R.id.number_1, R.id.number_2, R.id.number_3,
                R.id.number_4, R.id.number_5, R.id.number_6,
                R.id.number_7, R.id.number_8, R.id.number_9,
                R.id.number_0, R.id.point,
                R.id.plus, R.id.subtract, R.id.multiply, R.id.divide
        };
        for (int id : inputIds) {
            findViewById(id).setOnClickListener(this::onDigitClick);
        }

        //AC、←
        findViewById(R.id.clear).setOnClickListener(this::onClearClick);
        findViewById(R.id.back).setOnClickListener(this::onBackClick);

        //等號運算
        findViewById(R.id.equal).setOnClickListener(this::onEqualClick);
    }

    public void onDigitClick(View v) {
        if (amountBuilder.length() >= MAX_LEN) return;

        String key = ((TextView) v).getText().toString();
        char c = key.charAt(0);

        if (isOperator(c)) {
            if (amountBuilder.length() == 0) return;  // 開頭不允許運算符
            char last = amountBuilder.charAt(amountBuilder.length() - 1);
            if (isOperator(last)) {
                //連續運算符
                amountBuilder.setCharAt(amountBuilder.length() - 1, c);
            } else {
                amountBuilder.append(c);
            }

        } else if (c == '.') {
            if (amountBuilder.length() == 0) {
                amountBuilder.append("0.");
            } else {
                char last = amountBuilder.charAt(amountBuilder.length() - 1);
                if (isOperator(last)) {
                    amountBuilder.append("0.");
                } else {
                    //一個小數點
                    int i = amountBuilder.length() - 1;
                    while (i >= 0 && !isOperator(amountBuilder.charAt(i))) i--;
                    String segment = amountBuilder.substring(i + 1);
                    if (!segment.contains(".")) {
                        amountBuilder.append('.');
                    }
                }
            }

        } else {
            //開頭0
            if (c == '0' && amountBuilder.length() == 0) return;
            amountBuilder.append(c);
        }

        tvAmountDisplay.setText("NT$" + amountBuilder.toString());
    }

    public void onClearClick(View v) {
        amountBuilder.setLength(0);
        tvAmountDisplay.setText("NT$0");
    }

    public void onBackClick(View v) {
        if (amountBuilder.length() > 0) {
            amountBuilder.deleteCharAt(amountBuilder.length() - 1);
            String s = amountBuilder.length() > 0
                    ? amountBuilder.toString()
                    : "0";
            tvAmountDisplay.setText("NT$" + s);
        }
    }

    public void onEqualClick(View v) {
        try {
            //結尾符號刪掉
            while (amountBuilder.length() > 0) {
                char last = amountBuilder.charAt(amountBuilder.length() - 1);
                if (isOperator(last) || last == '.') {
                    amountBuilder.deleteCharAt(amountBuilder.length() - 1);
                } else break;
            }
            String expr = amountBuilder.toString()
                    .replace('×','*')
                    .replace('÷','/');
            double result = evaluate(expr);
            String resStr = formatResult(result);
            if (resStr.length() > MAX_LEN) {
                resStr = resStr.substring(0, MAX_LEN);
            }
            tvAmountDisplay.setText("NT$" + resStr);

            amountBuilder.setLength(0);
            amountBuilder.append(resStr);

        } catch (Exception e) {
            tvAmountDisplay.setText("Error");
            amountBuilder.setLength(0);
        }
    }

    private boolean isOperator(char c) {
        return c=='+'||c=='-'||c=='*'||c=='/'||c=='×'||c=='÷';
    }
    private double evaluate(String expr) {
        Stack<Double> vals = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i=0;
        while (i<expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c=='.') {
                int j=i;
                while (j<expr.length() &&
                        (Character.isDigit(expr.charAt(j))||expr.charAt(j)=='.')) j++;
                vals.push(Double.parseDouble(expr.substring(i,j)));
                i=j;
            } else {
                while (!ops.isEmpty() && precedence(ops.peek())>=precedence(c)) {
                    applyOp(vals, ops.pop());
                }
                ops.push(c);
                i++;
            }
        }
        while (!ops.isEmpty()) applyOp(vals, ops.pop());
        return vals.isEmpty()?0:vals.pop();
    }

    private int precedence(char op) {
        switch(op){
            case '+': case '-': return 1;
            case '*': case '/': return 2;
        }
        return 0;
    }

    private void applyOp(Stack<Double> vals, char op) {
        double b = vals.pop();
        double a = vals.isEmpty()?0:vals.pop();
        switch(op){
            case '+': vals.push(a+b); break;
            case '-': vals.push(a-b); break;
            case '*': vals.push(a*b); break;
            case '/': vals.push(b!=0?a/b:0); break;
        }
    }

    private String formatResult(double v) {
        return v==Math.floor(v)
                ? String.valueOf((long)v)
                : String.valueOf(v);
    }
}
