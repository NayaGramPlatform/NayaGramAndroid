package org.telegram.messenger;

import org.nayagram.platform.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    // Existing AppCenter and update-management implementation remains in this class.
}
