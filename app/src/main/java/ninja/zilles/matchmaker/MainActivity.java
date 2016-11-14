package ninja.zilles.matchmaker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = AppCompatActivity.class.getSimpleName();
    private final String NONE = "none";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mMatchmaker = database.getReference("matchmaker");
    DatabaseReference mGamesReference = database.getReference("games");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * This function is the callback of the "Find Match" button.   This function reads the current
     * value of the matchmaker storage location to determine if it thinks that we're the first arriver
     * or the second.
     * @param view
     */
    public void findMatch(View view) {
        mMatchmaker.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String matchmaker = dataSnapshot.getValue(String.class);
                Log.d(TAG, "mMatchmaker: " + matchmaker);

                if (matchmaker.equals(NONE)) {
                    findMatchFirstArriver();
                } else {
                    findMatchSecondArriver(matchmaker);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * The first arriver needs to create the game, add themselves to it, and then atomically
     * (i.e., using a transaction) verify that no one else has posted a game yet and post the game.
     * If it fails to post the game, it destroys the game.
     */
    private void findMatchFirstArriver() {
        String matchmaker;
        final DatabaseReference dbReference = mGamesReference.push();
        dbReference.push().setValue("player0");
        matchmaker = dbReference.getKey();
        final String newMatchmaker = matchmaker;

        mMatchmaker.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue(String.class).equals(NONE)) {
                    mutableData.setValue(newMatchmaker);
                    return Transaction.success(mutableData);
                }
                // someone beat us to posting a game, so fail and retry later
                return Transaction.abort();
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean commit, DataSnapshot dataSnapshot) {
                Toast.makeText(getApplicationContext(),
                        commit ? "transaction success" : "transaction failed",
                        Toast.LENGTH_SHORT).show();
                if (!commit) {
                    // we failed to post the game, so destroy the game so we don't leave trash.
                    dbReference.removeValue();
                }
            }
        });
    }

    /**
     * The second arriver needs atomically (i.e., with a transcation) verify that the game is
     * still available to join and then remove the game from the matchmaker.  It then adds
     * itself to the game, so that player0 gets a notification that the game was joined.
     * @param matchmaker
     */
    private void findMatchSecondArriver(final String matchmaker) {
        mMatchmaker.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue(String.class).equals(matchmaker)) {
                    mutableData.setValue(NONE);
                    return Transaction.success(mutableData);
                }
                // someone beat us to joining this game, so fail and retry later
                return Transaction.abort();
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed,
                                   DataSnapshot dataSnapshot) {
                if (committed) {
                    DatabaseReference gameReference = mGamesReference.child(matchmaker);
                    gameReference.push().setValue("player1");
                    mMatchmaker.setValue(NONE);
                }
            }
        });
    }
}
