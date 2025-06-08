package com.example.afinal.Fragment.Setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.afinal.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingResetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingResetFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SettingResetFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static SettingResetFragment newInstance(String param1, String param2) {
        SettingResetFragment fragment = new SettingResetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_reset, container, false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
        String userId = prefs.getString("userid", "0");
        if (!userId.equals("0")) {
            view.findViewById(R.id.tv_account_clear_personal).setOnClickListener(v -> {
                if (!userId.equals("0")) {
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("records")  // ← 確保這是你實際的集合名稱
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                                Toast.makeText(getContext(), "已刪除個人記帳紀錄", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "刪除失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                }
            });

            view.findViewById(R.id.tv_delete_account).setOnClickListener(v -> {
                DocumentReference userRef = db.collection("users").document(userId);

// 取得 Firebase 當前使用者
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

                if (firebaseUser != null) {
                    // 刪除兩個子集合
                    Task<Void> deleteRecordsTask = userRef.collection("records").get().continueWithTask(recordsTask -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : recordsTask.getResult()) {
                            batch.delete(doc.getReference());
                        }
                        return batch.commit();
                    });

                    Task<Void> deleteGroupsTask = userRef.collection("group").get().continueWithTask(groupsTask -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : groupsTask.getResult()) {
                            batch.delete(doc.getReference());
                        }
                        return batch.commit();
                    });

                    // 當兩個子集合都刪除完成後再刪掉 user document 和 Firebase Authentication 帳號
                    Tasks.whenAll(deleteRecordsTask, deleteGroupsTask)
                            .addOnSuccessListener(aVoid -> {
                                userRef.delete().addOnSuccessListener(aVoid2 -> {
                                    firebaseUser.delete()
                                            .addOnSuccessListener(aVoid3 -> {
                                                Toast.makeText(getContext(), "帳號已刪除", Toast.LENGTH_SHORT).show();
                                                // 跳轉回登入頁面或結束 Activity
//                                                Intent intent = new Intent(getActivity(), Manac.class);
//                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                startActivity(intent);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Firebase 使用者刪除失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "子集合刪除失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } else {
                    Toast.makeText(getContext(), "使用者尚未登入", Toast.LENGTH_SHORT).show();
                }

            });
        }
        return view;
    }


    private void deleteUserAccountAndData(FirebaseFirestore db, String userId) {
        DocumentReference userRef = db.collection("users").document(userId);

        // 刪除所有子集合
        userRef.collection("record").get().addOnSuccessListener(records -> {
            for (DocumentSnapshot doc : records) {
                doc.getReference().delete();
            }
        });

        userRef.collection("group").get().addOnSuccessListener(groups -> {
            for (DocumentSnapshot doc : groups) {
                doc.getReference().delete();
            }
        });


        // 刪除使用者 document 本身
        userRef.delete().addOnSuccessListener(aVoid -> {
            FirebaseAuth.getInstance().getCurrentUser().delete(); // 刪除 Firebase Auth 帳號（需重新登入才能刪）
            Toast.makeText(getContext(), "帳號已刪除", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "帳號刪除失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

}