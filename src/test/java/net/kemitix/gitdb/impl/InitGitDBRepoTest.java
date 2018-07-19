package net.kemitix.gitdb.impl;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

class InitGitDBRepoTest implements WithAssertions {

    @Test
    void utilityClassCannotBeInstantiated() throws NoSuchMethodException {
        final Constructor<InitGitDBRepo> constructor = InitGitDBRepo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatCode(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

}