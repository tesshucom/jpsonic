package com.tesshu.jpsonic.adapter;

import java.util.List;

import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.MusicFolderProvider;
import com.tesshu.jpsonic.service.scanner.MusicFolderServiceImpl;

import org.springframework.stereotype.Component;

@Component
public class MusicFolderProviderAdapter implements MusicFolderProvider {

    private final MusicFolderServiceImpl musicFolderService;

    public MusicFolderProviderAdapter(MusicFolderServiceImpl musicFolderService) {
        this.musicFolderService = musicFolderService;
    }

    @Override
    public List<MusicFolder> getGuestFolders() {
        return musicFolderService.getDomainGuestFolders();
    }
}
