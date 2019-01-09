package com.developer.wisdom.bakingapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.developer.wisdom.bakingapp.R;
import com.developer.wisdom.bakingapp.models.Ingredient;
import com.developer.wisdom.bakingapp.models.Recipe;
import com.developer.wisdom.bakingapp.models.Step;
import com.developer.wisdom.bakingapp.retrofit.APIServiceInterface;
import com.developer.wisdom.bakingapp.views.DetailActivity;
import com.developer.wisdom.bakingapp.adapters.MasterRecipeAdapter;
import com.developer.wisdom.bakingapp.utilities.APIUtils;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// This fragment displays all of the Baking Recipes
public class MasterListFragment extends Fragment implements MasterRecipeAdapter.RecipeAdapterOnClickHandler {

    private static final String LOG_TAG = MasterListFragment.class.getSimpleName();
    private static final String RECIPES_KEY = "recipes_key";
    private static final String RECIPE_NAME_KEY = "recipe_name_key";
    private static final String INGREDIENTS_KEY = "ingredients_key";
    private static final String STEPS_LIST_KEY = "steps_list_key";
    private static final String IS_TABLET_KEY = "two_pane";

    @BindView(R.id.recipe_recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    TextView mEmptyStateTextView;
    @BindView(R.id.avi)
    AVLoadingIndicatorView avi;

    private Context mContext;
    private ArrayList<Recipe> mRecipes;
    private ArrayList<Ingredient> mIngredients;
    private ArrayList<Step> mSteps;
    private APIServiceInterface mAPIService;
    private MasterRecipeAdapter mAdapter;
    private Boolean mTwoPane;

    // Mandatory empty constructor
    public MasterListFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    // Inflates the RecyclerView of all Recipes
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_master_list, container, false);
        ButterKnife.bind(this, rootView);
        mIngredients = new ArrayList<>();
        mRecipes = new ArrayList<>();
        mAPIService = APIUtils.getAPIInterface();
        mContext = getContext();
        mAdapter = new MasterRecipeAdapter(mContext, new ArrayList<Recipe>(0), this);


        if (savedInstanceState == null) {
            // Fetch the Recipes from the API
            setLayoutManager();
            fetchRecipes();
        } else {// fetch the saved values
            mTwoPane = savedInstanceState.getBoolean(IS_TABLET_KEY);
            mRecipes = savedInstanceState.getParcelableArrayList(RECIPES_KEY);
            setLayoutManager();
            if (mRecipes.size() != 0) {
                mAdapter.loadRecipes(mRecipes);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setHasFixedSize(true);
            } else { //The user has not saved anything (no internet)
                showNoInternetMessage();

                // Fetch the recipes now if the user has internet
                if (isConnected()) {
                    fetchRecipes();
                }
            }
        }

        return rootView;
    }

    // Method to fetch the recipes from the API through retrofit
    private void fetchRecipes() {
        showLoadingIndicator();
        mAPIService.getBakingRecipes().enqueue(new Callback<ArrayList<Recipe>>() {
            @Override
            public void onResponse(@NonNull Call<ArrayList<Recipe>> call, @NonNull Response<ArrayList<Recipe>> response) {
                if (response.isSuccessful()) {
                    mRecipes = response.body();
                    showRecipeRecyclerView();
                    if (mRecipes != null) {
                        // fetch the Recipes (results)
                        mAdapter.loadRecipes(mRecipes);
                        mRecyclerView.setAdapter(mAdapter);
                        mRecyclerView.setHasFixedSize(true);
                        Log.d("MasterListFragment", "recipes loaded from API");
                    } else {
                        Log.d("No recipes found", "no recipes found");
                    }
                } else {
                    Log.d(LOG_TAG, "ERROR with the API Response " + response.errorBody());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ArrayList<Recipe>> call, @NonNull Throwable t) {
                showNoInternetMessage();
                Log.d("Error", t.getMessage());
            }
        });
    }

    // When a recipe clicked, pass the
    // recipe's data in the Detail Activity
    @Override
    public void onClick(Recipe clickedRecipe) {
        Bundle bundle = new Bundle();
        mIngredients = clickedRecipe.getIngredients();
        mSteps = clickedRecipe.getSteps();
        bundle.putString(RECIPE_NAME_KEY, clickedRecipe.getName());
        bundle.putParcelableArrayList(INGREDIENTS_KEY, mIngredients);
        bundle.putParcelableArrayList(STEPS_LIST_KEY, mSteps);
        bundle.putBoolean(IS_TABLET_KEY, mTwoPane);
        final Intent intent = new Intent(mContext, DetailActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    // Save the current state of this fragment (the recipes)
    @Override
    public void onSaveInstanceState(@NonNull Bundle currentState) {
        currentState.putParcelableArrayList(RECIPES_KEY, mRecipes);
        currentState.putBoolean(IS_TABLET_KEY, mTwoPane);
    }

    public void setTwoPane(Boolean twoPane) {
        this.mTwoPane = twoPane;
    }

    // This method sets the layout for portrait or tablet
    private void setLayoutManager() {
        if (mTwoPane) {
            final GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3, LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(gridLayoutManager);
        }
        if (!mTwoPane) {
            final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(layoutManager);
        }
    }

    private void showLoadingIndicator() {
      //  loadingIndicator.setVisibility(View.VISIBLE);
        avi.hide();
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    private void showRecipeRecyclerView() {


        mEmptyStateTextView.setVisibility(View.GONE);
       // loadingIndicator.setVisibility(View.INVISIBLE);
        // Display the GridView (Display the movies)
        avi.hide();
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showNoInternetMessage() {
        if (!isConnected()) {

            avi.hide();
            mEmptyStateTextView.setText(R.string.no_internet);
            Toast.makeText(getContext(),"No Internet Connection ",Toast.LENGTH_SHORT).show();
        }
    }

    // This method checks if the user has Internet connection
    private boolean isConnected() {
        if (getActivity() == null) {
            return false;
        }
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        // boolean to check if there is a network connection
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}