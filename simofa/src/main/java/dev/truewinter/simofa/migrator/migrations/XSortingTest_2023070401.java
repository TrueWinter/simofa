package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.migrator.Migration;

@SuppressWarnings("unused")
public class XSortingTest_2023070401 extends Migration {
    @Override
    public void up() throws Exception {
        throw new Exception("Test");
    }

    @Override
    public void down() throws Exception {

    }

    @Override
    public boolean isActive() {
        // KEEP THIS FALSE UNLESS TESTING THE MIGRATION CODE
        return false;
    }
}
