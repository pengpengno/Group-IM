package com.github.im.server.workbench.common.context;

import java.util.Optional;

public interface CurrentWorkContextProvider {

    Optional<CurrentWorkContext> current();

    CurrentWorkContext require();
}
