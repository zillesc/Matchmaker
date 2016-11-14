# Matchmaker
An example using transactions with the Firebase Realtime Database to perform a primitive and non-scalable matchmaker for 2 player games.

The keys ideas to understanding this matchmaker are:
* There are two roles to this matchmaking process: the first arriver (who creates the game) and the second arriver (who joins the created game).
* There is a *matchmaker* storage location in the Realtime DB.  If the value of this location is "none" when you arrive, you are a first arriver.  Otherwise, you are a second arriver.
* Both player use transactions to update the matchmaker storage location to handle race conditions (multiple players trying to update the location at the same time).  This ensures that every game has one first arriver and one second arriver.
* The first arriver creates a game and sets the value of the matchmaker variable to the key of the created game.  It then listens to that game (code not included) to be notified when the other player joins the game.
* The second arriver reads the game reference from the matchmaker variable and then sets the value back to "none".  It then performs an action in the game (this code appends to it), to notify the first arriving player that it has arrived.

