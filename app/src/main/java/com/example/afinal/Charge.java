package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
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
import java.util.Calendar;
import java.util.List;
import java.util.Stack;

public class Charge extends AppCompatActivity {
    private static final int MAX_LEN = 20;
    private TextView tvAmountDisplay, tvNote, tvDateDisplay;
    private int     selectedIconRes;
    private String  selectedCategoryName;
    private StringBuilder amountBuilder = new StringBuilder();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_charge);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });

        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        tvNote          = findViewById(R.id.note);
        tvDateDisplay   = findViewById(R.id.tvDateDisplay);
        tvAmountDisplay.setText("NT$0");

        Calendar cal = Calendar.getInstance();
        String today = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH)+1,
                cal.get(Calendar.DAY_OF_MONTH));
        tvDateDisplay.setText(today);
        tvDateDisplay.setOnClickListener(v ->
                showDatePickerDialog(tvDateDisplay)
        );

        tvNote.setOnClickListener(v -> {
            TextView title = new TextView(this);
            title.setText("備註"); title.setGravity(Gravity.CENTER); title.setTextSize(20);
            EditText input = new EditText(this);
            input.setHint("請輸入備註");
            AlertDialog dlg = new AlertDialog.Builder(this)
                    .setCustomTitle(title)
                    .setView(input)
                    .setPositiveButton("確定", (d,w) -> {
                        String t = input.getText().toString().trim();
                        if (!t.isEmpty()) tvNote.setText(t);
                    })
                    .setNegativeButton("取消", null)
                    .create();
            dlg.show();
        });

        RecyclerView rv = findViewById(R.id.items);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        List<Category> cats = Arrays.asList(
                new Category(R.drawable.ic_add,      "新增"),
                new Category(R.drawable.ic_group,    "群組"),
                new Category(R.drawable.ic_location, "位置"),
                new Category(R.drawable.ic_setting,  "設定")
        );
        CategoryAdapter catAdapter = new CategoryAdapter(this, cats);
        catAdapter.setOnItemClickListener(c -> {
            selectedIconRes      = c.getIconResId();
            selectedCategoryName = c.getName();
        });
        rv.setAdapter(catAdapter);

        int[] ids = {
                R.id.number_1,R.id.number_2,R.id.number_3,
                R.id.number_4,R.id.number_5,R.id.number_6,
                R.id.number_7,R.id.number_8,R.id.number_9,
                R.id.number_0,R.id.point,
                R.id.plus,R.id.subtract,R.id.multiply,R.id.divide
        };
        for(int id:ids) findViewById(id).setOnClickListener(this::onDigitClick);
        findViewById(R.id.clear).setOnClickListener(this::onClearClick);
        findViewById(R.id.back ).setOnClickListener(this::onBackClick);
        findViewById(R.id.equal).setOnClickListener(this::onEqualClick);

        findViewById(R.id.confirm).setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("iconRes",      selectedIconRes);
            data.putExtra("categoryName", selectedCategoryName);
            data.putExtra("price",        tvAmountDisplay.getText().toString());
            data.putExtra("note",         tvNote.getText().toString());
            data.putExtra("date",         tvDateDisplay.getText().toString());
            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void onDigitClick(View v) {
        if(amountBuilder.length()>=MAX_LEN) return;
        String key = ((TextView)v).getText().toString();
        char c = key.charAt(0);
        if("+-*/×÷".indexOf(c)>=0) {
            if(amountBuilder.length()==0) return;
            char last = amountBuilder.charAt(amountBuilder.length()-1);
            if("+-*/×÷".indexOf(last)>=0)
                amountBuilder.setCharAt(amountBuilder.length()-1, c);
            else amountBuilder.append(c);
        } else if(c=='.') {
            if(amountBuilder.length()==0) amountBuilder.append("0.");
            else {
                char last = amountBuilder.charAt(amountBuilder.length()-1);
                if("+-*/×÷".indexOf(last)>=0) amountBuilder.append("0.");
                else {
                    int i=amountBuilder.length()-1;
                    while(i>=0 && "+-*/×÷".indexOf(amountBuilder.charAt(i))<0) i--;
                    String seg = amountBuilder.substring(i+1);
                    if(!seg.contains(".")) amountBuilder.append('.');
                }
            }
        } else {
            if(c=='0' && amountBuilder.length()==0) return;
            amountBuilder.append(c);
        }
        tvAmountDisplay.setText("NT$"+amountBuilder);
    }

    private void onClearClick(View v) {
        amountBuilder.setLength(0);
        tvAmountDisplay.setText("NT$0");
    }

    private void onBackClick(View v) {
        if(amountBuilder.length()>0){
            amountBuilder.deleteCharAt(amountBuilder.length()-1);
            String t = amountBuilder.length()>0 ? amountBuilder.toString() : "0";
            tvAmountDisplay.setText("NT$"+t);
        }
    }

    private void onEqualClick(View v) {
        try {
            while(amountBuilder.length()>0) {
                char last = amountBuilder.charAt(amountBuilder.length()-1);
                if("+-*/×÷".indexOf(last)>=0 || last=='.')
                    amountBuilder.deleteCharAt(amountBuilder.length()-1);
                else break;
            }
            String expr = amountBuilder.toString().replace('×','*').replace('÷','/');
            double res = evaluate(expr);
            String s = String.valueOf(res==Math.floor(res)?(long)res:res);
            if(s.length()>MAX_LEN) s=s.substring(0,MAX_LEN);
            tvAmountDisplay.setText("NT$"+s);
            amountBuilder.setLength(0);
            amountBuilder.append(s);
        } catch(Exception e){
            tvAmountDisplay.setText("Error");
            amountBuilder.setLength(0);
        }
    }

    private double evaluate(String expr){
        Stack<Double> vals=new Stack<>();
        Stack<Character> ops=new Stack<>();
        int i=0;
        while(i<expr.length()){
            char c=expr.charAt(i);
            if(Character.isDigit(c)||c=='.'){
                int j=i; while(j<expr.length()&&(Character.isDigit(expr.charAt(j))||expr.charAt(j)=='.')) j++;
                vals.push(Double.parseDouble(expr.substring(i,j)));
                i=j;
            } else {
                while(!ops.isEmpty() && precedence(ops.peek())>=precedence(c))
                    applyOp(vals, ops.pop());
                ops.push(c); i++;
            }
        }
        while(!ops.isEmpty()) applyOp(vals, ops.pop());
        return vals.isEmpty()?0:vals.pop();
    }

    private int precedence(char op){
        switch(op){case '+':case '-':return 1;case '*':case '/':return 2;}
        return 0;
    }

    private void applyOp(Stack<Double> v, char op){
        double b=v.pop(), a=v.isEmpty()?0:v.pop();
        switch(op){
            case '+': v.push(a+b); break;
            case '-': v.push(a-b); break;
            case '*': v.push(a*b); break;
            case '/': v.push(b!=0?a/b:0); break;
        }
    }

    private void showDatePickerDialog(TextView dateView){
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_day_picker,null);
        NumberPicker y = dv.findViewById(R.id.yearPicker);
        NumberPicker m = dv.findViewById(R.id.monthPicker);
        NumberPicker d = dv.findViewById(R.id.dayPicker);
        Calendar c=Calendar.getInstance();
        int cy=c.get(Calendar.YEAR), cm=c.get(Calendar.MONTH)+1, cd=c.get(Calendar.DAY_OF_MONTH);
        y.setMinValue(2000); y.setMaxValue(2100); y.setValue(cy);
        m.setMinValue(1);    m.setMaxValue(12);   m.setValue(cm);
        d.setMinValue(1);    d.setMaxValue(31);   d.setValue(cd);
        AlertDialog dlg=new AlertDialog.Builder(this).setView(dv).create();
        Button ok=dv.findViewById(R.id.btnOK), no=dv.findViewById(R.id.btnCancel);
        ok.setOnClickListener(v->{
            dateView.setText(String.format("%04d-%02d-%02d", y.getValue(), m.getValue(), d.getValue()));
            dlg.dismiss();
        });
        no.setOnClickListener(v->dlg.dismiss());
        dlg.show();
    }
}
