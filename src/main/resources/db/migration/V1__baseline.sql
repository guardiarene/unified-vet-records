-- Baseline schema generated from the entity model with Hibernate schema-generation scripts
-- (MySQLDialect, MySQL 8.4). Constraint names renamed from Hibernate's hashes to
-- uk_<table>_<column> and fk_<table>_<column>.

    create table anamnesis (
        active bit not null,
        last_birth_date date,
        last_deworming_date date,
        last_heat_date date,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        allergies TEXT,
        created_by varchar(255) not null,
        current_medications TEXT,
        deleted_by varchar(255),
        diet TEXT,
        previous_diseases TEXT,
        surgeries TEXT,
        updated_by varchar(255),
        reproductive_status enum ('INTACT','LACTATING','NEUTERED','PREGNANT','UNKNOWN'),
        primary key (id)
    ) engine=InnoDB;

    create table app_user (
        active bit not null,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        work_clinic_id bigint,
        phone varchar(20) not null,
        first_name varchar(50) not null,
        last_name varchar(50) not null,
        password varchar(72) not null,
        email varchar(254) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        role enum ('CUSTOMER','RECEPTIONIST','SYSTEM_ADMINISTRATOR','VETERINARIAN') not null,
        primary key (id)
    ) engine=InnoDB;

    create table appointment (
        active bit not null,
        date date not null,
        time time(0) not null,
        version integer,
        created_at datetime(6) not null,
        customer_id bigint not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        medical_service_id bigint not null,
        pet_id bigint not null,
        updated_at datetime(6),
        veterinary_clinic_id bigint not null,
        visit_id bigint,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        appointment_status enum ('CANCELLED','COMPLETED','CONFIRMED','NO_SHOW','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table clinical_exam (
        active bit not null,
        heart_rate integer not null,
        pulse integer not null,
        respiratory_rate integer not null,
        temperature float(23) not null,
        version integer,
        weight float(23) not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        mucosal_membranes varchar(100) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        description TEXT,
        updated_by varchar(255),
        temperament enum ('AGGRESSIVE','AGITATED','ANXIOUS','CALM','FRIENDLY','UNKNOWN') not null,
        primary key (id)
    ) engine=InnoDB;

    create table diagnostic (
        active bit not null,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        visit_id bigint not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table diagnostic_problems (
        diagnostic_id bigint not null,
        problem varchar(255)
    ) engine=InnoDB;

    create table medical_record (
        active bit not null,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        pet_id bigint not null,
        updated_at datetime(6),
        created_by varchar(255) not null,
        deleted_by varchar(255),
        general_observations TEXT,
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table medical_service (
        active bit not null,
        version integer,
        clinic_id bigint not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        name varchar(100) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        description TEXT,
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table pet (
        active bit not null,
        birth_date date not null,
        version integer,
        weight float(23) not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        owner_id bigint not null,
        updated_at datetime(6),
        name varchar(50) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        breed enum ('ABYSSINIAN','ABYSSINIAN_GUINEA_PIG','AMERICAN_GUINEA_PIG','ANGELFISH','ANGORA_RABBIT','BALL_PYTHON','BEAGLE','BEARDED_DRAGON','BENGAL','BETTA','BORDER_COLLIE','BRITISH_SHORTHAIR','BUDGERIGAR','CANARY','CHIHUAHUA','CHINESE_HAMSTER','COCKATIEL','COCKATOO','CORN_SNAKE','DACHSHUND','DUTCH','DWARF_HAMSTER','ENGLISH_BULLDOG','FRENCH_BULLDOG','GERMAN_SHEPHERD','GOLDEN_RETRIEVER','GOLDFISH','GUPPY','HOLLAND_LOP','LABRADOR_RETRIEVER','LEOPARD_GECKO','LIONHEAD','LOVEBIRD','MAINE_COON','MALTESE','MINI_LOP','MIXED_BIRD','MIXED_CAT','MIXED_DOG','MIXED_FISH','MIXED_GUINEA_PIG','MIXED_HAMSTER','MIXED_RABBIT','MIXED_REPTILE','NEON_TETRA','OTHER_BREED','PARROT','PERSIAN','PERUVIAN_GUINEA_PIG','POODLE','RAGDOLL','RED_EARED_SLIDER','REX_RABBIT','ROBOROVSKI','ROTTWEILER','RUSSIAN_BLUE','SCOTTISH_FOLD','SHIH_TZU','SIAMESE','SIBERIAN_HUSKY','SYRIAN_HAMSTER','TEDDY_GUINEA_PIG','YORKSHIRE_TERRIER') not null,
        sex enum ('FEMALE','MALE') not null,
        species enum ('BIRD','CAT','DOG','FISH','GUINEA_PIG','HAMSTER','OTHER','RABBIT','REPTILE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table treatment (
        active bit not null,
        end_date date,
        start_date date not null,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        visit_id bigint not null,
        frequency varchar(50) not null,
        route varchar(50) not null,
        product varchar(100) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table vaccine (
        active bit not null,
        application_date date not null,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        medical_record_id bigint not null,
        updated_at datetime(6),
        visit_id bigint,
        batch varchar(50) not null,
        dose varchar(50) not null,
        brand varchar(100) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table veterinary_clinic (
        active bit not null,
        version integer,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        owner_id bigint not null,
        updated_at datetime(6),
        phone varchar(20) not null,
        name varchar(100) not null,
        email varchar(254) not null,
        address varchar(255) not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table visit (
        active bit not null,
        date date not null,
        version integer,
        anamnesis_id bigint,
        clinical_exam_id bigint,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        medical_record_id bigint not null,
        updated_at datetime(6),
        veterinarian_id bigint not null,
        created_by varchar(255) not null,
        deleted_by varchar(255),
        observations TEXT,
        reason_for_visit varchar(255) not null,
        updated_by varchar(255),
        visit_type enum ('CONSULTATION','EMERGENCY','FOLLOW_UP','HOSPITALIZATION','LABORATORY','OTHER','ROUTINE_CHECKUP','SURGERY','VACCINATION') not null,
        primary key (id)
    ) engine=InnoDB;

    alter table app_user
       add constraint uk_app_user_email unique (email);

    alter table appointment
       add constraint uk_appointment_visit_id unique (visit_id);

    alter table medical_record
       add constraint uk_medical_record_pet_id unique (pet_id);

    alter table veterinary_clinic
       add constraint uk_veterinary_clinic_email unique (email);

    alter table visit
       add constraint uk_visit_anamnesis_id unique (anamnesis_id);

    alter table visit
       add constraint uk_visit_clinical_exam_id unique (clinical_exam_id);

    alter table app_user
       add constraint fk_app_user_work_clinic_id
       foreign key (work_clinic_id)
       references veterinary_clinic (id);

    alter table appointment
       add constraint fk_appointment_veterinary_clinic_id
       foreign key (veterinary_clinic_id)
       references veterinary_clinic (id);

    alter table appointment
       add constraint fk_appointment_customer_id
       foreign key (customer_id)
       references app_user (id);

    alter table appointment
       add constraint fk_appointment_medical_service_id
       foreign key (medical_service_id)
       references medical_service (id);

    alter table appointment
       add constraint fk_appointment_pet_id
       foreign key (pet_id)
       references pet (id);

    alter table appointment
       add constraint fk_appointment_visit_id
       foreign key (visit_id)
       references visit (id);

    alter table diagnostic
       add constraint fk_diagnostic_visit_id
       foreign key (visit_id)
       references visit (id);

    alter table diagnostic_problems
       add constraint fk_diagnostic_problems_diagnostic_id
       foreign key (diagnostic_id)
       references diagnostic (id);

    alter table medical_record
       add constraint fk_medical_record_pet_id
       foreign key (pet_id)
       references pet (id);

    alter table medical_service
       add constraint fk_medical_service_clinic_id
       foreign key (clinic_id)
       references veterinary_clinic (id);

    alter table pet
       add constraint fk_pet_owner_id
       foreign key (owner_id)
       references app_user (id);

    alter table treatment
       add constraint fk_treatment_visit_id
       foreign key (visit_id)
       references visit (id);

    alter table vaccine
       add constraint fk_vaccine_medical_record_id
       foreign key (medical_record_id)
       references medical_record (id);

    alter table vaccine
       add constraint fk_vaccine_visit_id
       foreign key (visit_id)
       references visit (id);

    alter table veterinary_clinic
       add constraint fk_veterinary_clinic_owner_id
       foreign key (owner_id)
       references app_user (id);

    alter table visit
       add constraint fk_visit_anamnesis_id
       foreign key (anamnesis_id)
       references anamnesis (id);

    alter table visit
       add constraint fk_visit_clinical_exam_id
       foreign key (clinical_exam_id)
       references clinical_exam (id);

    alter table visit
       add constraint fk_visit_medical_record_id
       foreign key (medical_record_id)
       references medical_record (id);

    alter table visit
       add constraint fk_visit_veterinarian_id
       foreign key (veterinarian_id)
       references app_user (id);
