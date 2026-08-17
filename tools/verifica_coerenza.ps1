$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# La relazione finale è la fonte dei requisiti e non viene modificata.
$relation = 'C:\Users\loren\Downloads\finale.docx'
$projectRoot = Split-Path -Parent $PSScriptRoot
$schema = Join-Path $projectRoot 'doc\sql\01_schema_completo.sql'
$sourceRoot = Join-Path $projectRoot 'src\main\java'

if (-not (Test-Path -LiteralPath $relation)) {
    throw "Relazione non trovata: $relation"
}
if (-not (Test-Path -LiteralPath $schema)) {
    throw "Schema non trovato: $schema"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($relation)
try {
    $entry = $archive.GetEntry('word/document.xml')
    if ($null -eq $entry) {
        throw 'Il documento non contiene word/document.xml'
    }
    $reader = [IO.StreamReader]::new($entry.Open())
    try {
        [xml]$document = $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
} finally {
    $archive.Dispose()
}

$namespace = [Xml.XmlNamespaceManager]::new($document.NameTable)
$namespace.AddNamespace(
    'w',
    'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
)
$paragraphs = foreach (
    $paragraph in $document.SelectNodes('//w:body/w:p', $namespace)
) {
    (
        $paragraph.SelectNodes('.//w:t', $namespace) |
        ForEach-Object { $_.InnerText }
    ) -join ''
}
$chapterFour = [Array]::IndexOf(
    $paragraphs,
    'Capitolo 4 – Progettazione logica'
)
if ($chapterFour -lt 0) {
    throw 'Inizio del capitolo 4 non trovato nella relazione'
}
$requirements = ($paragraphs[0..($chapterFour - 1)] -join "`n")
$schemaText = Get-Content -LiteralPath $schema -Raw
$sourceText = (
    Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' |
    Get-Content -Raw
) -join "`n"

$entities = @(
    'UTENTE',
    'ATLETA',
    'ISTRUTTORE',
    'TIPO_ABBONAMENTO',
    'ABBONAMENTO',
    'TIPO_ATTIVITA',
    'ATTIVITA_PROGRAMMATA',
    'ISCRIZIONE_ATTIVITA',
    'ACCESSO_NUOTO_LIBERO',
    'VASCA',
    'CORSIA',
    'CLUB_SPORTIVO',
    'SQUADRA',
    'APPARTENENZA_SQUADRA',
    'INCARICO_SQUADRA'
)
$associations = @(
    'COMPATIBILITA',
    'UTILIZZA',
    'ASSEGNATO_A',
    'SVOLGE'
)
$requiredTriggers = @(
    'TR_COMPATIBILITA_BI',
    'TR_ABBONAMENTO_BI',
    'TR_ISCRIZIONE_BI',
    'TR_ACCESSO_BI',
    'TR_UTILIZZA_BI',
    'TR_ASSEGNATO_A_BI',
    'TR_APPARTENENZA_BI',
    'TR_INCARICO_BI',
    'TR_SVOLGE_BI'
)

$errors = [Collections.Generic.List[string]]::new()
foreach ($entity in $entities) {
    if ($requirements -notmatch [Regex]::Escape($entity)) {
        $errors.Add("Entità non trovata nella relazione: $entity")
    }
    if ($schemaText -notmatch (
            'CREATE TABLE IF NOT EXISTS\s+' + [Regex]::Escape($entity)
        )) {
        $errors.Add("Tabella non trovata nello schema: $entity")
    }
}
foreach ($association in $associations) {
    if ($requirements -notmatch [Regex]::Escape($association)) {
        $errors.Add(
            "Associazione non trovata nella relazione: $association"
        )
    }
    if ($schemaText -notmatch (
            'CREATE TABLE IF NOT EXISTS\s+' +
            [Regex]::Escape($association)
        )) {
        $errors.Add(
            "Associazione non trovata nello schema: $association"
        )
    }
}
foreach ($trigger in $requiredTriggers) {
    if ($schemaText -notmatch (
            'CREATE TRIGGER\s+' + [Regex]::Escape($trigger)
        )) {
        $errors.Add("Trigger di vincolo non trovato: $trigger")
    }
}

$sections = @(
    'Abbonamenti',
    'Attività',
    'Accessi',
    'Struttura',
    'Club e squadre',
    'Riepiloghi'
)
foreach ($section in $sections) {
    if ($sourceText -notmatch [Regex]::Escape($section)) {
        $errors.Add("Sezione applicativa non trovata: $section")
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Coerenza strutturale verificata: $($entities.Count) entità, $($associations.Count) associazioni, $($requiredTriggers.Count) gruppi di vincoli e $($sections.Count) sezioni applicative."
