<!-- id: 53674402 -->

# List of BIDS Apps

Working through all the entries in the [list of BIDS Apps](http://bids-apps.neuroimaging.io/apps/) as of 2018-04-05.

| BIDS App                                                                                          | Docker Image                                                                                  | Command                                                                                    | Link to section below                                       | Do I think we can make a command? |
|:--------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------|:------------------------------------------------------------|:----------------------------------|
| [BIDS-Apps/freesurfer](https://github.com/BIDS-Apps/freesurfer)                                   | [bids/freesurfer](https://hub.docker.com/r/bids/freesurfer)                                   |                                                                                            | [Freesurfer](#freesurfer)                                   | No                                |
| [BIDS-Apps/ndmg](https://github.com/BIDS-Apps/ndmg)                                               | [bids/ndmg](https://hub.docker.com/r/bids/ndmg)                                               |                                                                                            | [ndmg](#ndmg)                                               | Need connectome data              |
| [BIDS-Apps/BROCCOLI](https://github.com/BIDS-Apps/BROCCOLI)                                       | [bids/broccoli](https://hub.docker.com/r/bids/broccoli)                                       |                                                                                            | [BROCCOLI](#broccoli)                                       | Maybe                             |
| [BIDS-Apps/FibreDensityAndCrosssection](https://github.com/BIDS-Apps/FibreDensityAndCrosssection) | [bids/fibredensityandcrosssection](https://hub.docker.com/r/bids/fibredensityandcrosssection) |                                                                                            | [FibreDensityAndCrosssection](#FibreDensityAndCrosssection) | No idea                           |
| [BIDS-Apps/SPM](https://github.com/BIDS-Apps/SPM)                                                 | [bids/spm](https://hub.docker.com/r/bids/spm)                                                 |                                                                                            | [SPM](#SPM)                                                 | No idea                           |
| [poldracklab/mriqc](https://github.com/poldracklab/mriqc)                                         | [poldracklab/mriqc](https://hub.docker.com/r/poldracklab/mriqc)                               | [bids-mriqc](https://github.com/NrgXnat/docker-images/blob/master/bids-mriqc/command.json) | [mriqc](#mriqc)                                             | Done                              |
| [BIDS-Apps/QAP](https://github.com/BIDS-Apps/QAP)                                                 | [bids/qap](https://hub.docker.com/r/bids/qap)                                                 |                                                                                            | [QAP](#QAP)                                                 | Yes                               |
| [BIDS-Apps/CPAC](https://github.com/BIDS-Apps/CPAC)                                               | [bids/cpac](https://hub.docker.com/r/bids/cpac)                                               |                                                                                            | [C-PAC](#C-PAC)                                             | Maybe                             |
| [BIDS-Apps/hyperalignment](https://github.com/BIDS-Apps/hyperalignment)                           | [bids/hyperalignment](https://hub.docker.com/r/bids/hyperalignment)                           |                                                                                            | [hyperalignment](#hyperalignment)                           | Probably                          |
| [BIDS-Apps/mindboggle](https://github.com/BIDS-Apps/mindboggle)                                   | [bids/mindboggle](https://hub.docker.com/r/bids/mindboggle)                                   |                                                                                            | [mindboggle](#mindboggle)                                   | Need Freesurfer data              |
| [BIDS-Apps/MRtrix3_connectome](https://github.com/BIDS-Apps/MRtrix3_connectome)                   | [bids/mrtrix3_connectome](https://hub.docker.com/r/bids/mrtrix3_connectome)                   |                                                                                            | [MRtrix3_connectome](#MRtrix3-connectome)                   | Need connectome data              |
| [BIDS-Apps/rs_signal_extract](https://github.com/BIDS-Apps/rs_signal_extract)                     | [bids/rs_signal_extract](https://hub.docker.com/r/bids/rs_signal_extract)                     |                                                                                            | [rs_signal_extract](#rs-signal-extract)                     | No idea                           |
| [BIDS-Apps/aa](https://github.com/BIDS-Apps/aa)                                                   | [bids/aa](https://hub.docker.com/r/bids/aa)                                                   |                                                                                            | [aa](#aa)                                                   | Probably not                      |
| [BIDS-Apps/niak](https://github.com/BIDS-Apps/niak)                                               | [bids/niak](https://hub.docker.com/r/bids/niak)                                               |                                                                                            | [niak](#niak)                                               | Probably not                      |
| [BIDS-Apps/oppni](https://github.com/BIDS-Apps/oppni)                                             | [bids/oppni](https://hub.docker.com/r/bids/oppni)                                             |                                                                                            | [oppni](#oppni)                                             | Yes                               |
| [poldracklab/fmriprep](https://github.com/poldracklab/fmriprep)                                   | [poldracklab/fmriprep](https://hub.docker.com/r/poldracklab/fmriprep)                         |                                                                                            | [fMRIPrep](#fMRIPrep)                                       | Yes                               |
| [BIDS-Apps/brainiak-srm](https://github.com/BIDS-Apps/brainiak-srm)                               | [bids/brainiak-srm](https://hub.docker.com/r/bids/brainiak-srm)                               |                                                                                            | [brainiak-srm](#brainiak-srm)                               | Probably                          |
| [BIDS-Apps/nipypelines](https://github.com/BIDS-Apps/nipypelines)                                 | [bids/nipypelines](https://hub.docker.com/r/bids/nipypelines)                                 |                                                                                            | [nipypelines](#nipypelines)                                 | No idea                           |
| [BIDS-Apps/HCPPipelines](https://github.com/BIDS-Apps/HCPPipelines)                               | [bids/hcppipelines](https://hub.docker.com/r/bids/hcppipelines)                               |                                                                                            | [HCPPipelines](#HCPPipelines)                               | Need connectome data              |
| [BIDS-Apps/MAGeTbrain](https://github.com/BIDS-Apps/MAGeTbrain)                                   | [bids/magetbrain](https://hub.docker.com/r/bids/magetbrain)                                   |                                                                                            | [MAGeTbrain](#MAGeTbrain)                                   | Yes                               |
| [BIDS-Apps/tracula](https://github.com/BIDS-Apps/tracula)                                         | [bids/tracula](https://hub.docker.com/r/bids/tracula)                                         |                                                                                            | [tracula](#tracula)                                         | Need Freesurfer data              |
| [BIDS-Apps/baracus](https://github.com/BIDS-Apps/baracus)                                         | [bids/baracus](https://hub.docker.com/r/bids/baracus)                                         |                                                                                            | [baracus](#baracus)                                         | Need Freesurfer data              |
| [BIDS-Apps/antsCorticalThickness](https://github.com/BIDS-Apps/antsCorticalThickness)             | [bids/antscorticalthickness](https://hub.docker.com/r/bids/antscorticalthickness)             |                                                                                            | [antsCorticalThickness](#antsCorticalThickness)             | No idea                           |
| [BIDS-Apps/DPARSF](https://github.com/BIDS-Apps/DPARSF)                                           | [bids/dparsf](https://hub.docker.com/r/bids/dparsf)                                           |                                                                                            | [DPARSF](#DPARSF)                                           | Probably not                      |
| [BIDS-Apps/afni_proc](https://github.com/BIDS-Apps/afni_proc)                                     | [bids/afni_proc](https://hub.docker.com/r/bids/afni_proc)                                     |                                                                                            | [afni_proc](#afni-proc)                                     | No                                |


# Freesurfer
[Repo](https://github.com/BIDS-Apps/freesurfer)

## What is it?

Freesurfer 6.0.0

## Can we run it?

Not sure. We need a way to get a Freesurfer license file in there.

And we don't have a great way to make a Freesurfer assessor from the outputs. So anything we do with FS now would be a functional regression from the pipeline.

# BROCCOLI
[Repo](https://github.com/BIDS-Apps/BROCCOLI) [Docs](https://github.com/wanderine/BROCCOLI/raw/master/documentation/broccoli.pdf) (PDF)

## What is it?

> BROCCOLI is a software for analysis of fMRI (functional magnetic resonance imaging) data and is written in OpenCL (Open Computing Language).

## Can we run it?

I am not sure. It is unclear to me whether this takes raw or pre-processed fMRI scans. If raw, we can do it.

# ndmg
[Repo](https://github.com/BIDS-Apps/ndmg) [Docs](http://m2g.io/)

## What is it?

> NeuroData’s MR Graphs package, ndmg (pronounced "nutmeg"), is the successor of the MRCAP, MIGRAINE, and m2g pipelines. ndmg combines dMRI and sMRI data from a single subject to estimate a high-level connectome reliably and scalably.

## Can we run it?

We would need HCP-type data that we can use to process an entire connectome. There are sample data sets on the Docs page.

Other than that, looks like yes.

# FibreDensityAndCrosssection
[Repo](https://github.com/BIDS-Apps/FibreDensityAndCrosssection) [Docs](http://userdocs.mrtrix.org/)

## What is it?

> This BIDS App enables group analysis of diffusion MRI data by performing a Fixel-Based Analysis (FBA) of Fibre Density, Fibre Cross-section and a combined measure (Fibre Density & Cross-section).
>
> The analysis pipeline relies primarily on the MRtrix3 software package.

## Can we run it?

I don't know. This needs diffusion data, so we would need to get some of that. And it has a weird multi-step process where you have to do a participant-level analysis, then group, then another participant analysis, then group, etc.

> This fixel-based analysis pipeline has been broken up into several stages, each defined by the "analysis level" positional argument. Each level is labelled as either participant or group. Participant levels can be run on different subjects independently, while group level analysis is performed on all subjects within the group. The order in which the analysis should be run is participant1, group1, participant2, group2, participant3, group3, particpant4, group4.

# SPM
[Repo](https://github.com/BIDS-Apps/SPM)

## What is it?

> BIDS App containing an instance of the [SPM12 software](http://www.fil.ion.ucl.ac.uk/spm/).

## Can we run it?

I don't know. I don't know what this does. Is this intended to be an automated thing? Or an easily-accessible code library?

# mriqc
[Repo](https://github.com/BIDS-Apps/mriqc) [Docs](http://mriqc.readthedocs.io)

## What is it?

> a functional magnetic resonance imaging (fMRI) data pre-processing pipeline.

## Can we run it?

Yes. I have already written a command for it.

# QAP
[Repo](https://github.com/BIDS-Apps/QAP) [Docs](http://preprocessed-connectomes-project.org/quality-assessment-protocol/)

## What is it?

> The QAP package allows you to obtain spatial and anatomical data quality measures for your own data.

## Can we run it?

Looks like yes.

# C-PAC
[Repo](https://github.com/BIDS-Apps/CPAC) [Docs](http://fcp-indi.github.io/docs/user/index.html)

## What is it?

> The Configurable Pipeline for the Analysis of Connectomes (C-PAC) is a software for performing high-throughput preprocessing and analysis of functional connectomes data using high-performance computers.

> C-PAC makes it possible to use a single configuration file to launch a factorial number of pipelines differing with respect to specific processing steps (e.g., spatial/temporal filter settings, global correction strategies, motion correction strategies, group analysis models).

## Can we run it?

Maybe. But given that its strengths lie with launching a huge number of parallel processes for data exploration, I don't know that we want to.

# hyperalignment
[Repo](https://github.com/BIDS-Apps/hyperalignment) [Docs](http://www.pymvpa.org/generated/mvpa2.algorithms.hyperalignment.Hyperalignment.html)

## What is it?

> Hyperalignment is a functional alignment method that aligns subjects' brain data in a high-dimensional space of voxels/features.

Trains a model that takes an input dataset to an aligned output dataset.

## Can we run it?

Probably. I don't know exatly what the output is (aligned images? trained models? both?) but it seems likely.

# mindboggle
[Repo](https://github.com/BIDS-Apps/mindboggle) [Docs](http://mindboggle.readthedocs.io)

## What is it?

> Mindboggle’s open source brain morphometry platform takes in preprocessed T1-weighted MRI data, and outputs volume, surface, and tabular data containing label, feature, and shape information for further analysis.

Note: by "preprocessed T1-weighted MRI data", they mean BIDS-formatted Freesurfer data.

## Can we run it?

Yes, if we can get the Freesurfer data.

We would need to write a new setup command to take the Freesurfer data and BIDS-ify it. See the [note on the README](https://github.com/BIDS-Apps/mindboggle#docker):
> This app assumes the freesurfer BIDS-App has been run.
>
> The folder structure of the mindboggle input should look like:

    bids_dir/
      derivatives/
        freesurfer/
          sub-*/
            mri/
            label/
            surf/

Also note that a lot of the paths in the docs look like example paths, but they are not. They should be used literally.

> \[It is\] important to mount to a directory in `/home/jovyan/` because you are not root in this Docker image.

# MRtrix3_connectome
[Repo](https://github.com/BIDS-Apps/MRtrix3_connectome) [Docs](http://mrtrix.readthedocs.org/)

## What is it?

> Generate subject connectomes from raw image data and perform inter-subject connection density normalisation, using tools provided in the *MRtrix3* software package.

## Can we run it?

We could use it to generate subject connectomes. Not sure about the "inter-subject connectome density normalization" because I'm not sure about running across subjects. Could work. Just not sure.

# rs_signal_extract
[Repo](https://github.com/BIDS-Apps/rs_signal_extract)

## What is it?

> we use the nilearn NiftiLabelsMasker to extract time-series on a parcellation, or "max-prob" atlas

## Can we run it?

No idea. I don't know what a parcellation is or how we get one.

# aa
[Repo](https://github.com/BIDS-Apps/aa) [Docs](http://automaticanalysis.org/)

## What is it?

> framework for medical image analysis designed to allow users to achieve an efficient analysis workflow

Looks like a matlab-based pipeline engine. Users can write little scripts to do analysis "using off-the-shelf recipes with minimal coding".

## Can we run it?

Maybe. But I don't think we want to. Without the scripts, this seems a little useless.

# niak
[Repo](https://github.com/BIDS-Apps/niak) [Docs](http://niak.simexp-lab.org/)

## What is it?

> The neuroimaging analysis kit (NIAK) is a library of pipelines for the preprocessing and mining of large functional neuroimaging data, using GNU Octave or Matlab(r), and distributed under a MIT license. This includes but is not limited to the preprocessing pipeline implemented in this app.

> This app implements a pipeline for preprocessing structural and functional MRI datasets.

## Can we run it?

Sure.

# oppni
[Repo](https://github.com/BIDS-Apps/oppni)

## What is it?

> OPPNI (Optimization of Preprocessing Pipelines for NeuroImaging) does fast optimization of preprocessing pipelines for BOLD fMRI (Blood Oxygenation Level Dependent functional MRI). OPPNI identifies the set of preprocessing steps (“pipeline”) specific to each dataset, which optimizes quality metrics of cross-validated Prediction and/or spatial-pattern Reproducibility for a range of analysis models (Strother et al., 2002, 2004; LaConte et al., 2003; Shaw et al., 2003)... The pipeline software can also be used for simple automated batch-processing of fMRI datasets, if no appropriate predictive analysis model is available to do optimization (e.g. some resting-state connectivity studies).

## Can we run it?

Looks like it.

# fMRIPrep
[Repo](https://github.com/BIDS-Apps/fmriprep) [Docs](http://fmriprep.readthedocs.io)

## What is it?
Runs fMRI pre-processing. Settings and options are minimal, as it infers a lot about your data from the BIDS metadata.

## Can we run it?
Yes, but with a caveat. We can run it only by disabling Freesurfer (using the `--no-freesurfer` option). To use Freesurfer, you have to provide a Freesurfer license file.

...Or maybe we could use a setup command for this. Make an image that is just busybox with a FS license. Its only job is to spit out the FS license file. Write a setup command for it, and run that setup command it on some mount (which mount? what else is in it? can we run setup containers on empty mounts?) before running fmriprep. Could possibly work, maybe.

Whether the setup command works or not, we should be able to run this without FS pretty easily.

# brainiak-srm
[BIDS App Repo](https://github.com/BIDS-Apps/brainiak-srm) [Source Repo](https://github.com/brainiak/brainiak) [Docs](http://brainiak.org/docs/brainiak.funcalign.html)

## What is it?

> Shared Response Model (SRM) from the Brain Imaging Analysis Kit (BrainIAK).
>
> The Shared Response Model (SRM) is a method for aligning fMRI scans from several subjects by assuming similar functional behavior in the brain. The voxels of each subject are mapped to voxels of other subjects by projecting the information from each subject into a low-dimensional space.

## Can we run it?

Probably?

# nipypelines
[Repo](https://github.com/BIDS-Apps/nipypelines)

## What is it?

> A preprocessing workflow for functional timeseries data.

## Can we run it?

Don't know. The README links to mindboggle. Is this different from the [mindboggle BIDS App](#mindboggle)?

# HCPPipelines
[Repo](https://github.com/BIDS-Apps/HCPPipelines)

## What is it?

> BIDS App wrapper for HCP Pipelines

## Can we run it?

Yes, if we get HCP data in BIDS format.

Note:
> To convert DICOMs from your HCP-Style (CMRR) acquisitions to BIDS try using [heudiconv](https://github.com/nipy/heudiconv) with this [heuristic file](https://github.com/nipy/heudiconv/blob/master/heuristics/cmrr_heuristic.py).

Accepts FS license on command line.

# MAGeTbrain
[Repo](https://github.com/BIDS-Apps/MAGeTbrain)

## What is it?

> This pipeline takes in native-space T1 or T2 (or multiple co-registered modalities) brain images and volumetrically segments them using the MAGeTbrain algorithm.

## Can we run it?

Yes

# tracula
[Repo](https://github.com/BIDS-Apps/tracula) [Docs](https://surfer.nmr.mgh.harvard.edu/fswiki/Tracula)

## What is it?

> implements Freesurfer's TRACULA (TRActs Constrained by UnderLying Anatomy) tool for cross-sectional as well as longitudinal (multi session) input data.

## Can we run it?

Needs a DWI scan and either a T1 or previously-run Freesurfer data.

Interestingly, it can accept the FS licence on the command line. Doesn't need a file. We could make a hidden site-wide input for that.

# baracus
[Repo](https://github.com/BIDS-Apps/baracus)

## What is it?

BARACUS: Brain-Age Regression Analysis and Computation Utility Software

> This BIDS App predicts brain age, based on data from Freesurfer 5.3. It combines data from cortical thickness, cortical surface area, and subcortical information (see Liem et al., 2017).

## Can we run it?

Probably. It says that it either needs BIDS-formatted FS data, or it will run FS 5.3.0 (no -HCP) first.

Can accept license on the command line like tracula.

# antsCorticalThickness
[BIDS App Repo](https://github.com/BIDS-Apps/antsCorticalThickness) [Source Repo](https://github.com/ANTsX/ANTs)

## What is it?
### ANTs in general
> Advanced Normalization Tools (ANTs) computes high-dimensional mappings to capture the statistics of brain structure and function.
>
> ANTs allows one to organize, visualize and statistically explore large biomedical image sets.

### This BIDS App

> This BIDS App runs ANTs cortical thickness estimation pipeline.

## Can we run it?

Sure. Maybe. I don't know.

# DPARSF
[Repo](https://github.com/BIDS-Apps/DPARSF) [Docs](http://rfmri.org/DPARSF)

## What is it?

> Data Processing Assistant for Resting-State fMRI (DPARSF) is a convenient plug-in software within [DPABI](http://rfmri.org/dpabi), which is based on SPM.

What is DPABI, you may ask?

> DPABI is a GNU/GPL* toolbox for Data Processing & Analysis of Brain Imaging, evolved from DPARSF (Data Processing Assistant for Resting-State fMRI).

This looks like a bunch of matlab / SPM processing routines with a GUI only a programmer could love.
![DPARSF GUI](http://rfmri.org/sites/default/files/images/DPARSFV4_1.png)

## Can we run it?

I don't know. It is unclear what, exactly, it does.

Before we could run this we would need to provide a way to customize the inputs. They don't take anything on the command line except for an input file.

> If you want to customize your processing, please setup a `.m` file (`Config_DPARSF.m` is an example) according to the instructions at: http://rfmri.org/content/configurations-dparsfarun. Then use `--config` to specify the path. E.g.,

    docker run -i --rm -v /data/MyDataBIDS:/inputs:ro -v /data/DPARSFResults:/outputs dparsfdocker /inputs /outputs participant --participant_label 01 02 04 05 07 09 10 --config /inputs/Config_DPARSF.m


# afni_proc
[Repo](https://github.com/BIDS-Apps/afni_proc) [Docs](https://afni.nimh.nih.gov/pub/dist/doc/program_help/afni_proc.py.html)

## What is it?

> This is a prototype AFNI bids app implmenting participant level preprocessing with afni_proc.py. This pipeline is currently doing temporal alignment, nonlinear registration to standard space, bluring of 4 mm, masking, and scaling for all epis in the input bids dataset

## Can we run it?

I don't think we want to, necessarily. As the blurb says, this is a "prototype". AFNI is a whole big thing, and just a single AFNI script isn't worth that much.
