package com.goldtea.sales;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.goldtea.sales.data.firestore.FirestoreManager;
import com.goldtea.sales.ui.dashboard.DashboardFragment;
import com.goldtea.sales.ui.newsale.NewSaleFragment;
import com.goldtea.sales.ui.reports.ReportsFragment;
import com.goldtea.sales.ui.settings.SettingsFragment;
import com.goldtea.sales.ui.viewsales.ViewSalesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable Edge-to-Edge and apply insets
        setContentView(R.layout.activity_main);

        android.view.View topHeader = findViewById(R.id.top_header);
        android.widget.TextView headerTitle = findViewById(R.id.header_title);
        topHeader.setBackgroundColor(getResources().getColor(R.color.surface));

        // Global Notepad Button
        findViewById(R.id.header_notepad_button).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new com.goldtea.sales.ui.notes.NotesFragment())
                    .addToBackStack(null)
                    .commit();
            headerTitle.setText("Notepad");
        });

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);
        bottomNav.setBackgroundColor(getResources().getColor(R.color.surface));

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            headerTitle.setText("GOLD Tea");
        }
    }

    private final NavigationBarView.OnItemSelectedListener navListener =
            new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    String title = "GOLD Tea";
                    
                    int itemId = item.getItemId();
                    if (itemId == R.id.nav_dashboard) {
                        selectedFragment = new DashboardFragment();
                        title = "GOLD Tea";
                    } else if (itemId == R.id.nav_new_sale) {
                        selectedFragment = new NewSaleFragment();
                        title = "New Sale";
                    } else if (itemId == R.id.nav_view_sales) {
                        selectedFragment = new ViewSalesFragment();
                        title = "View Sales";
                    } else if (itemId == R.id.nav_reports) {
                        selectedFragment = new ReportsFragment();
                        title = "Reports";
                    } else if (itemId == R.id.nav_settings) {
                        selectedFragment = new SettingsFragment();
                        title = "Settings";
                    }
                    
                    if (selectedFragment != null) {
                        ((android.widget.TextView)findViewById(R.id.header_title)).setText(title);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                        return true;
                    }
                    
                    return false;
                }
            };
}
