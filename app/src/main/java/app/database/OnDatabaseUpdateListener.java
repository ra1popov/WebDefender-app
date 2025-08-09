package app.database;

public interface OnDatabaseUpdateListener {

    void onDatabaseUpdate();

    void onDatabaseUpdated(int oldVersion, int newVersion);

}
