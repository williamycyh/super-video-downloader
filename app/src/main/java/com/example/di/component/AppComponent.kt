package com.example.di.component

import com.example.DLApplication
import com.example.di.module.ActivityBindingModule
import com.example.di.module.AppModule
import com.example.di.module.DatabaseModule
import com.example.di.module.MyWorkerModule
import com.example.di.module.NetworkModule
import com.example.di.module.RepositoryModule
import com.example.di.module.UtilModule
import com.example.di.module.ViewModelModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton


@Singleton
@Component(
    modules = [AndroidSupportInjectionModule::class, AppModule::class, ActivityBindingModule::class, UtilModule::class,
        DatabaseModule::class, NetworkModule::class, RepositoryModule::class, ViewModelModule::class, MyWorkerModule::class]
)
interface AppComponent : AndroidInjector<DLApplication> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: DLApplication): Builder

        fun build(): AppComponent
    }
}