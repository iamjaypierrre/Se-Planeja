package com.example.seplaneja.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Movie;
import android.os.Bundle;

import com.example.seplaneja.adapter.AdapterMovimentacao;
import com.example.seplaneja.config.ConfiguracaoFirebase;
import com.example.seplaneja.databinding.ActivityPrincipalBinding;
import com.example.seplaneja.helper.Base64Custom;
import com.example.seplaneja.model.Movimentacao;
import com.example.seplaneja.model.Usuario;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.seplaneja.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PrincipalActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView textoBoasVindas, textoSaldo;
    private ActivityPrincipalBinding binding;
    private Double despesaTotal = 0.0;
    private Double receitaTotal = 0.0;
    private Double resumoUsuario = 0.0;

    private FirebaseAuth autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private DatabaseReference usuarioRef;
    private ValueEventListener valueEventListenerUsuario;
    private ValueEventListener valueEventListenerMovimentacoes;

    private RecyclerView recyclerView;
    private AdapterMovimentacao adapterMovimentacao;
    private List<Movimentacao> movimentacoes = new ArrayList<>();
    private Movimentacao movimentacao;
    private DatabaseReference movimentacaoRef;
    private String mesAnoSelecionado;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrincipalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(binding.toolbar);

        textoBoasVindas = findViewById(R.id.textBoasVindas);
        textoSaldo = findViewById(R.id.textSaldo);
        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.recyclerMovimentos);
        configuraCalendarView();
        swipe();

        //Configurar adapter
        adapterMovimentacao = new AdapterMovimentacao(movimentacoes, this);

        //Configurar RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapterMovimentacao);

    }

    public void swipe(){

        ItemTouchHelper.Callback itemTouch = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                excluirMovimentacao(viewHolder);
            }
        };

        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);

    }



    public void excluirMovimentacao(RecyclerView.ViewHolder viewHolder){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        //Configura AlertDialog
        alertDialog.setTitle("Excluir Movimentacao da Conta");
        alertDialog.setMessage("Voce tem certeza que deseja realmente excluir essa movimentacao da conta?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int position = viewHolder.getAdapterPosition();
                movimentacao = movimentacoes.get(position);

                String emailUsuario = autenticacao.getCurrentUser().getEmail();
                String idUsuario = Base64Custom.codificarBase64(emailUsuario);
                movimentacaoRef = firebaseRef.child("movimentacao")
                        .child(idUsuario)
                        .child(mesAnoSelecionado);

                movimentacaoRef.child(movimentacao.getKey()).removeValue();
                adapterMovimentacao.notifyItemRemoved(position);
                atualizarSaldo();
            }
        });

        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Toast.makeText(PrincipalActivity.this,
                        "Cancelado",
                        Toast.LENGTH_SHORT).show();
                adapterMovimentacao.notifyDataSetChanged();

            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

    }

    public void atualizarSaldo(){

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        if (movimentacao.getTipo().equals("r")){
            receitaTotal = receitaTotal - movimentacao.getValor();
            usuarioRef.child("receitaTotal").setValue(receitaTotal);
        }

        if (movimentacao.getTipo().equals("d")){
            despesaTotal = despesaTotal - movimentacao.getValor();
            usuarioRef.child("despesaTotal").setValue(despesaTotal);

        }

    }


    public void recuperarMovimentacoes() {

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        movimentacaoRef = firebaseRef.child("movimentacao")
                .child(idUsuario)
                .child(mesAnoSelecionado);

        valueEventListenerMovimentacoes = movimentacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                movimentacoes.clear();
                for (DataSnapshot dados : snapshot.getChildren()) {

                    Movimentacao movimentacao = dados.getValue(Movimentacao.class);
                    movimentacao.setKey(dados.getKey());
                    movimentacoes.add(movimentacao);
                }

                adapterMovimentacao.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void recuperarResumo() {

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        //Log.i("Evento", "evento foi adicionado");
        valueEventListenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario usuario = snapshot.getValue(Usuario.class);

                despesaTotal = usuario.getDespesaTotal();
                receitaTotal = usuario.getReceitaTotal();
                resumoUsuario = receitaTotal - despesaTotal;

                DecimalFormat decimalFormat = new DecimalFormat("0.##");
                String resultadoFormatado = decimalFormat.format(resumoUsuario);

                textoSaldo.setText("R$ " + resultadoFormatado);
                textoBoasVindas.setText("Ol??, " + usuario.getNome());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSair:
                autenticacao.signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void adicionarDespesa(View view) {
        startActivity(new Intent(this, DespesasActivity.class));

    }

    public void adicionarReceita(View view) {
        startActivity(new Intent(this, ReceitasActivity.class));
    }

    public void configuraCalendarView() {

        //valores para os meses
        CharSequence meses[] = {"Janeiro", "Fevereiro", "Mar??o", "Abril", "Maio", "Junho", "Julho",
                "Agosto", "Setembro", "Otubro", "Novembro", "Dezembro"};
        calendarView.setTitleMonths(meses);

        CalendarDay dataAtual = calendarView.getCurrentDate();
        String mesSelecionado = String.format("%02d", dataAtual.getMonth() + 1);
        mesAnoSelecionado = mesSelecionado + "" + dataAtual.getYear();

        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                String mesSelecionado = String.format("%02d", date.getMonth() + 1);
                mesAnoSelecionado = mesSelecionado + "" + date.getYear();

                movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
                recuperarMovimentacoes();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        recuperarResumo();
        recuperarMovimentacoes();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Evento", "evento foi removido");
        usuarioRef.removeEventListener(valueEventListenerUsuario);
        movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
    }


}



