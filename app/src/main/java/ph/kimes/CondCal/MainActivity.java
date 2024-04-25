package ph.kimes.CondCal;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import org.json.JSONException;
import org.json.JSONObject;

import ph.kimes.condcal.CondCal;
import ph.kimes.condcal.CondCalException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        AppCompatButton btnCompute = findViewById(R.id.btn_compute);
        btnCompute.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_compute:
                try {
                    String amountCheckin = "((Number(weight<=10))=(100+systemFee))^^((Number(1==1))=(((weight*8)-10)*8+100+systemFee))";

                    JSONObject params = new JSONObject();
                    params.put("systemFee", 10);
                    params.put("weight", 11);

                    CondCal condCal = new CondCal(amountCheckin, params);
                    System.out.println("Value is: " + condCal.getValue());
                } catch (JSONException | CondCalException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
