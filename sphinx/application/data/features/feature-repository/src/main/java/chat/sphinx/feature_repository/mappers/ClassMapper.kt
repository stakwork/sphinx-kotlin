package chat.sphinx.feature_repository.mappers

internal abstract class ClassMapper<DTO, DBO, Presenter> {

    abstract fun fromDTOtoDBO(dto: DTO): DBO
    abstract fun fromDTOtoPresenter(dto: DTO): Presenter
    abstract fun fromDTOsToDBOs(dtos: List<DTO>): List<DBO>
    abstract fun fromDTOsToPresenters(dtos: List<DTO>): List<Presenter>

    abstract fun fromDBOtoPresenter(dbo: DBO): Presenter
    abstract fun fromDBOsToPresenters(dbos: List<DBO>): List<Presenter>

    abstract fun fromPresenterToDBO(presenter: Presenter): DBO
    abstract fun fromPresentersToDBOs(presenters: List<Presenter>): List<DBO>
}
