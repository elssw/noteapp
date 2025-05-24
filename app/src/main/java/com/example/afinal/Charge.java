package com.example.afinal;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.afinal.model.Category;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Charge extends AppCompatActivity {
    private static final int MAX_LEN = 20;      // 最多 20 個字元
    private TextView tvAmountDisplay;
    private StringBuilder amountBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_charge);

        // Edge-to-Edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. 金額顯示
        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        tvAmountDisplay.setText("NT$0");

        // 2. RecyclerView：4 欄 GridLayout + 類別圖示
        RecyclerView rv = findViewById(R.id.items);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        List<Category> cats = Arrays.asList(
                new Category(R.drawable.ic_add,      "新增"),
                new Category(R.drawable.ic_group,    "群組"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_setting,  "設定")
        );
        rv.setAdapter(new CategoryAdapter(cats));

        // 3. 數字／運算符按鍵註冊
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

        // 4. 註冊清除（AC）與退格（←）
        findViewById(R.id.clear).setOnClickListener(this::onClearClick);
        findViewById(R.id.back).setOnClickListener(this::onBackClick);

        // 5. 註冊等號運算
        findViewById(R.id.equal).setOnClickListener(this::onEqualClick);
    }

    public void onDigitClick(View v) {
        if (amountBuilder.length() >= MAX_LEN) return;

        String key = ((TextView) v).getText().toString();
        char c = key.charAt(0);

        if (isOperator(c)) {
            if (amountBuilder.length() == 0) return;            // 不允許開頭
            char last = amountBuilder.charAt(amountBuilder.length() - 1);
            if (isOperator(last)) {
                // 連續運算符 → 替換為最新
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
                    // 檢查同段是否已有小數點
                    int i = amountBuilder.length() - 1;
                    while (i >= 0 && !isOperator(amountBuilder.charAt(i))) i--;
                    String segment = amountBuilder.substring(i + 1);
                    if (!segment.contains(".")) {
                        amountBuilder.append('.');
                    }
                }
            }

        } else {
            // 數字：開頭多次按 0 不處理
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
            String text = amountBuilder.length() > 0 ? amountBuilder.toString() : "0";
            tvAmountDisplay.setText("NT$" + text);
        }
    }

    public void onEqualClick(View v) {
        try {
            // 去除末尾運算符或小數點
            while (amountBuilder.length() > 0) {
                char last = amountBuilder.charAt(amountBuilder.length() - 1);
                if (isOperator(last) || last == '.') {
                    amountBuilder.deleteCharAt(amountBuilder.length() - 1);
                } else break;
            }

            // 準備表達式
            String expr = amountBuilder.toString()
                    .replace('×','*')
                    .replace('÷','/');

            double result = evaluate(expr);
            String resStr = formatResult(result);
            // 限制輸出長度
            if (resStr.length() > MAX_LEN) {
                resStr = resStr.substring(0, MAX_LEN);
            }

            tvAmountDisplay.setText("NT$" + resStr);

            // 重置 buffer 為計算結果
            amountBuilder.setLength(0);
            amountBuilder.append(resStr);

        } catch (Exception e) {
            tvAmountDisplay.setText("Error");
            amountBuilder.setLength(0);
        }
    }

    private boolean isOperator(char c) {
        return c=='+' || c=='-' || c=='*' || c=='/' || c=='×' || c=='÷';
    }

    private double evaluate(String expr) {
        Stack<Double> values = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j)=='.')) j++;
                values.push(Double.parseDouble(expr.substring(i, j)));
                i = j;
            } else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c)) {
                    applyOp(values, ops.pop());
                }
                ops.push(c);
                i++;
            }
        }
        while (!ops.isEmpty()) {
            applyOp(values, ops.pop());
        }
        return values.isEmpty() ? 0 : values.pop();
    }

    private int precedence(char op) {
        switch (op) {
            case '+': case '-': return 1;
            case '*': case '/': return 2;
        }
        return 0;
    }

    private void applyOp(Stack<Double> values, char op) {
        double b = values.pop();
        double a = values.isEmpty() ? 0 : values.pop();
        switch (op) {
            case '+': values.push(a + b); break;
            case '-': values.push(a - b); break;
            case '*': values.push(a * b); break;
            case '/': values.push(b != 0 ? a / b : 0); break;
        }
    }

    private String formatResult(double val) {
        if (val == Math.floor(val)) {
            return String.valueOf((long)val);
        } else {
            return String.valueOf(val);
        }
    }
}
